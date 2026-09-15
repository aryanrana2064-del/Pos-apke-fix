package com.vx7.khatapro.core.utils

import android.content.Context
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal, dependency-free reader/writer for the OOXML (.xlsx) spreadsheet format.
 * Writes plain, valid workbooks using inline strings (no sharedStrings.xml needed),
 * and reads workbooks back robustly even if the file was opened, edited and
 * re-saved by Excel itself (which switches to sharedStrings.xml and may
 * reorder sheets/columns) — everything is resolved by name, not by position.
 */
object ExcelUtils {

    sealed class Cell {
        data class Str(val value: String) : Cell()
        data class Num(val value: Double) : Cell()
    }

    data class ParsedSheet(val headers: List<String>, val rows: List<Map<String, String>>)
    data class ParsedWorkbook(val sheets: Map<String, ParsedSheet>)

    // ---------------------------------------------------------------- WRITER

    fun buildWorkbookBytes(sheetsInOrder: List<Pair<String, Pair<List<String>, List<List<Cell>>>>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun putEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val sheetOverrides = sheetsInOrder.indices.joinToString("\n") { i ->
                "<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            }
            putEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$sheetOverrides
</Types>"""
            )

            putEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            val sheetTags = sheetsInOrder.indices.joinToString("\n") { i ->
                "<sheet name=\"${escXml(sheetsInOrder[i].first)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
            }
            putEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>
$sheetTags
</sheets>
</workbook>"""
            )

            val relTags = sheetsInOrder.indices.joinToString("\n") { i ->
                "<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>"
            }
            putEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$relTags
</Relationships>"""
            )

            sheetsInOrder.forEachIndexed { i, (_, headerAndRows) ->
                val (headers, rows) = headerAndRows
                putEntry("xl/worksheets/sheet${i + 1}.xml", buildSheetXml(headers, rows))
            }
        }
        return baos.toByteArray()
    }

    private fun buildSheetXml(headers: List<String>, rows: List<List<Cell>>): String {
        val headerRow = "<row r=\"1\">" + headers.mapIndexed { i, h -> cellXml(i, 1, Cell.Str(h)) }.joinToString("") + "</row>"
        val dataRows = rows.mapIndexed { rIdx, row ->
            val rowNum = rIdx + 2
            "<row r=\"$rowNum\">" + row.mapIndexed { cIdx, cell -> cellXml(cIdx, rowNum, cell) }.joinToString("") + "</row>"
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
$headerRow
$dataRows
</sheetData>
</worksheet>"""
    }

    private fun cellXml(colIdx: Int, rowIdx: Int, cell: Cell): String {
        val ref = "${colLetters(colIdx)}$rowIdx"
        return when (cell) {
            is Cell.Str -> if (cell.value.isEmpty()) "" else
                "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escXml(cell.value)}</t></is></c>"
            is Cell.Num -> "<c r=\"$ref\"><v>${formatNum(cell.value)}</v></c>"
        }
    }

    private fun formatNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    private fun escXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun colLetters(index0: Int): String {
        var n = index0 + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, ('A' + rem))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun colLettersToIndex(letters: String): Int {
        var result = 0
        for (ch in letters) result = result * 26 + (ch - 'A' + 1)
        return result - 1
    }

    // ---------------------------------------------------------------- READER

    fun readWorkbook(file: File): ParsedWorkbook {
        ZipFile(file).use { zip ->
            fun entryText(name: String): String? =
                zip.getEntry(name)?.let { zip.getInputStream(it).bufferedReader(Charsets.UTF_8).readText() }

            val workbookXml = entryText("xl/workbook.xml")
                ?: throw IllegalArgumentException("Not a valid Excel (.xlsx) file")
            val relsXml = entryText("xl/_rels/workbook.xml.rels").orEmpty()
            val sharedStrings = entryText("xl/sharedStrings.xml")?.let { parseSharedStrings(it) } ?: emptyList()

            val sheetNameToRid = parseSheetList(workbookXml)
            val ridToTarget = parseRelationships(relsXml)

            val sheetsMap = mutableMapOf<String, ParsedSheet>()
            for ((name, rid) in sheetNameToRid) {
                val target = ridToTarget[rid] ?: continue
                val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
                val sheetXml = entryText(path) ?: continue
                sheetsMap[name] = parseSheetXml(sheetXml, sharedStrings)
            }
            return ParsedWorkbook(sheetsMap)
        }
    }

    private fun parseXmlDoc(xml: String): Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(xml)))

    private fun parseSharedStrings(xml: String): List<String> {
        val doc = parseXmlDoc(xml)
        val siNodes = doc.getElementsByTagName("si")
        val list = mutableListOf<String>()
        for (i in 0 until siNodes.length) {
            val si = siNodes.item(i) as Element
            val tNodes = si.getElementsByTagName("t")
            val sb = StringBuilder()
            for (j in 0 until tNodes.length) sb.append(tNodes.item(j).textContent)
            list.add(sb.toString())
        }
        return list
    }

    private fun parseSheetList(workbookXml: String): List<Pair<String, String>> {
        val doc = parseXmlDoc(workbookXml)
        val sheetNodes = doc.getElementsByTagName("sheet")
        val result = mutableListOf<Pair<String, String>>()
        for (i in 0 until sheetNodes.length) {
            val el = sheetNodes.item(i) as Element
            val name = el.getAttribute("name")
            val rid = if (el.hasAttribute("r:id")) el.getAttribute("r:id") else el.getAttribute("id")
            result.add(name to rid)
        }
        return result
    }

    private fun parseRelationships(relsXml: String): Map<String, String> {
        if (relsXml.isBlank()) return emptyMap()
        val doc = parseXmlDoc(relsXml)
        val relNodes = doc.getElementsByTagName("Relationship")
        val map = mutableMapOf<String, String>()
        for (i in 0 until relNodes.length) {
            val el = relNodes.item(i) as Element
            map[el.getAttribute("Id")] = el.getAttribute("Target")
        }
        return map
    }

    private fun parseSheetXml(sheetXml: String, sharedStrings: List<String>): ParsedSheet {
        val doc = parseXmlDoc(sheetXml)
        val rowNodes = doc.getElementsByTagName("row")

        var headers: List<String> = emptyList()
        var headerIndexToName: Map<Int, String> = emptyMap()
        val dataRows = mutableListOf<Map<String, String>>()

        for (r in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(r) as Element
            val cellNodes = rowEl.getElementsByTagName("c")
            val values = mutableMapOf<Int, String>()

            for (c in 0 until cellNodes.length) {
                val cEl = cellNodes.item(c) as Element
                val ref = cEl.getAttribute("r")
                val colLetters = ref.takeWhile { it.isLetter() }
                if (colLetters.isEmpty()) continue
                val colIdx = colLettersToIndex(colLetters)
                val type = cEl.getAttribute("t")

                val value: String = when (type) {
                    "s" -> {
                        val vNodes = cEl.getElementsByTagName("v")
                        val idx = if (vNodes.length > 0) vNodes.item(0).textContent.toIntOrNull() else null
                        idx?.let { sharedStrings.getOrNull(it) } ?: ""
                    }
                    "inlineStr" -> {
                        val tNodes = cEl.getElementsByTagName("t")
                        val sb = StringBuilder()
                        for (j in 0 until tNodes.length) sb.append(tNodes.item(j).textContent)
                        sb.toString()
                    }
                    else -> {
                        val vNodes = cEl.getElementsByTagName("v")
                        if (vNodes.length > 0) vNodes.item(0).textContent else ""
                    }
                }
                values[colIdx] = value
            }

            if (r == 0) {
                headerIndexToName = values.filterValues { it.isNotBlank() }
                headers = headerIndexToName.entries.sortedBy { it.key }.map { it.value }
            } else {
                if (values.values.all { it.isBlank() }) continue
                val rowMap = mutableMapOf<String, String>()
                for ((idx, name) in headerIndexToName) rowMap[name] = values[idx].orEmpty()
                dataRows.add(rowMap)
            }
        }
        return ParsedSheet(headers, dataRows)
    }

    // ---------------------------------------------------------------- FILE HELPERS

    fun writeToFile(context: Context, sheetsInOrder: List<Pair<String, Pair<List<String>, List<List<Cell>>>>>): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "khatapro_excel_$timeStamp.xlsx")
        FileOutputStream(file).use { it.write(buildWorkbookBytes(sheetsInOrder)) }
        return file
    }
}
