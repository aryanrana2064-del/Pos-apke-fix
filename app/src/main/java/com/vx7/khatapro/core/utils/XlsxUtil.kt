package com.vx7.khatapro.core.utils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * One sheet of tabular data to write into an .xlsx workbook.
 * [rows] includes the header row as rows[0]. [numericColumns] lists the
 * 0-based column indices (in data rows only) that should be written as
 * real numbers rather than text — kept explicit rather than auto-detected
 * so things like phone numbers or IDs with leading zeros never get
 * silently mangled by Excel's numeric parsing.
 */
data class XlsxSheet(
    val name: String,
    val rows: List<List<String>>,
    val numericColumns: Set<Int> = emptySet()
)

/**
 * Writes a plain OOXML (.xlsx) workbook by hand — just a zip of small XML
 * parts. No POI or any other third-party library, so the app stays fully
 * offline and dependency-free. Every cell is written as either an inline
 * string or a plain number; no styles beyond Excel's defaults are needed.
 */
object XlsxWriter {

    fun write(file: File, sheets: List<XlsxSheet>) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            fun entry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            entry("[Content_Types].xml", contentTypesXml(sheets.size))
            entry("_rels/.rels", rootRelsXml())
            entry("xl/workbook.xml", workbookXml(sheets.map { it.name }))
            entry("xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            entry("xl/styles.xml", stylesXml())

            sheets.forEachIndexed { index, sheet ->
                entry("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
    }

    private fun contentTypesXml(sheetCount: Int): String {
        val overrides = buildString {
            for (i in 1..sheetCount) {
                append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            }
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>$overrides</Types>"""
    }

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbookXml(sheetNames: List<String>): String {
        val sheetTags = buildString {
            sheetNames.forEachIndexed { index, name ->
                append("<sheet name=\"${escapeXml(name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>")
            }
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>$sheetTags</sheets></workbook>"""
    }

    private fun workbookRelsXml(sheetCount: Int): String {
        val rels = buildString {
            for (i in 1..sheetCount) {
                append("<Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
            }
            append("<Relationship Id=\"rId${sheetCount + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$rels</Relationships>"""
    }

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs></styleSheet>"""

    private fun sheetXml(sheet: XlsxSheet): String {
        val body = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            sheet.rows.forEachIndexed { rowIndex, row ->
                val rowNum = rowIndex + 1
                val isHeader = rowIndex == 0
                append("<row r=\"$rowNum\">")
                row.forEachIndexed { colIndex, value ->
                    val cellRef = "${columnLetter(colIndex + 1)}$rowNum"
                    val treatAsNumber = !isHeader && colIndex in sheet.numericColumns && value.toDoubleOrNull() != null
                    if (treatAsNumber) {
                        append("<c r=\"$cellRef\"><v>$value</v></c>")
                    } else {
                        append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(value)}</t></is></c>")
                    }
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        return body
    }

    private fun columnLetter(oneBasedIndex: Int): String {
        var n = oneBasedIndex
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, ('A' + rem))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

/**
 * Reads sheets back out of an .xlsx file by name. Handles both the plain
 * inline-string format [XlsxWriter] produces AND the shared-strings format
 * that Excel/Google Sheets rewrite files into after the user edits and
 * re-saves them — that re-save round trip is the main real-world import
 * scenario, so both must work.
 */
object XlsxReader {

    /** Returns a map of sheet name -> rows (each row a list of cell text, row 0 = header). */
    fun readAllSheets(input: InputStream): Map<String, List<List<String>>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val workbookXml = entries["xl/workbook.xml"]
            ?: throw IllegalArgumentException("Not a valid Excel (.xlsx) file")
        val relsXml = entries["xl/_rels/workbook.xml.rels"]
            ?: throw IllegalArgumentException("Not a valid Excel (.xlsx) file")
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()

        val sheetNameToRid = parseWorkbookSheets(workbookXml)
        val ridToTarget = parseRelationships(relsXml)

        val result = mutableMapOf<String, List<List<String>>>()
        sheetNameToRid.forEach { (name, rid) ->
            val target = ridToTarget[rid] ?: return@forEach
            val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
            val sheetBytes = entries[path] ?: return@forEach
            result[name] = parseSheetXml(sheetBytes, sharedStrings)
        }
        return result
    }

    private fun newParser(): XmlPullParser =
        XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()

    private fun parseWorkbookSheets(xml: ByteArray): List<Pair<String, String>> {
        val parser = newParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")
        val result = mutableListOf<Pair<String, String>>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: ""
                var rId: String? = null
                for (i in 0 until parser.attributeCount) {
                    val attrName = parser.getAttributeName(i)
                    if (attrName == "r:id" || attrName.endsWith(":id")) {
                        rId = parser.getAttributeValue(i)
                    }
                }
                if (rId != null) result.add(name to rId)
            }
            event = parser.next()
        }
        return result
    }

    private fun parseRelationships(xml: ByteArray): Map<String, String> {
        val parser = newParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")
        val map = mutableMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) map[id] = target
            }
            event = parser.next()
        }
        return map
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val parser = newParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")
        val result = mutableListOf<String>()
        var current: StringBuilder? = null
        var inSi = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") {
                    inSi = true
                    current = StringBuilder()
                }
                XmlPullParser.TEXT -> if (inSi) current?.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    result.add(current?.toString() ?: "")
                    inSi = false
                    current = null
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheetXml(xml: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val parser = newParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")

        val rows = mutableListOf<MutableList<String>>()
        var currentRow: MutableList<String>? = null
        var currentCellRef: String? = null
        var currentCellType: String? = null
        var currentText: StringBuilder? = null
        var capturingText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        currentCellRef = parser.getAttributeValue(null, "r")
                        currentCellType = parser.getAttributeValue(null, "t")
                        currentText = null
                    }
                    "v", "t" -> {
                        capturingText = true
                        if (currentText == null) currentText = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> if (capturingText) currentText?.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> capturingText = false
                    "c" -> {
                        val colIndex = currentCellRef?.let { colIndexFromRef(it) } ?: (currentRow?.size ?: 0)
                        val raw = currentText?.toString() ?: ""
                        val resolved = if (currentCellType == "s") {
                            raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                        } else raw
                        currentRow?.let { row ->
                            while (row.size <= colIndex) row.add("")
                            row[colIndex] = resolved
                        }
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun colIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return (col - 1).coerceAtLeast(0)
    }
}
