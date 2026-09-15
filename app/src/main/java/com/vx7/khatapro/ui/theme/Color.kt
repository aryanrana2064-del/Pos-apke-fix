package com.vx7.khatapro.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// "VX7 Neon" palette — electric blue + neon green on near-black, matching
// the VX7 Khata Pro brand mark (glowing ring, dark badge, blue-to-green glow).
// ---------------------------------------------------------------------------

// Dark Theme Colors (primary, brand-forward theme — the app defaults to this feel)
val KhataPrimaryDark = Color(0xFF4C8DFF) // Electric blue
val KhataOnPrimaryDark = Color(0xFF00203C)
val KhataPrimaryContainerDark = Color(0xFF0F3D73)
val KhataOnPrimaryContainerDark = Color(0xFFD6E7FF)

val KhataSecondaryDark = Color(0xFF2FE38A) // Neon green
val KhataOnSecondaryDark = Color(0xFF00391D)
val KhataSecondaryContainerDark = Color(0xFF0E5A38)
val KhataOnSecondaryContainerDark = Color(0xFFC6FFE0)

val KhataTertiaryDark = Color(0xFF7FD8FF) // Cyan glow accent
val KhataOnTertiaryDark = Color(0xFF00344A)
val KhataTertiaryContainerDark = Color(0xFF184F68)
val KhataOnTertiaryContainerDark = Color(0xFFD3F3FF)

val KhataErrorDark = Color(0xFFFF6B6B)
val KhataOnErrorDark = Color(0xFF4A0E0E)
val KhataErrorContainerDark = Color(0xFF7A1F1F)
val KhataOnErrorContainerDark = Color(0xFFFFDAD6)

val KhataBackgroundDark = Color(0xFF070B14) // Near-black, matches launcher badge
val KhataOnBackgroundDark = Color(0xFFE8EFFB)
val KhataSurfaceDark = Color(0xFF0E1520)
val KhataOnSurfaceDark = Color(0xFFE8EFFB)
val KhataSurfaceVariantDark = Color(0xFF1B2634)
val KhataOnSurfaceVariantDark = Color(0xFFA9B8CC)
val KhataOutlineDark = Color(0xFF3C4C60)

// Light Theme Colors (same brand hues, tuned to sit on white for daytime use)
val KhataPrimaryLight = Color(0xFF1B5FD6) // Electric blue, deepened for contrast on white
val KhataOnPrimaryLight = Color(0xFFFFFFFF)
val KhataPrimaryContainerLight = Color(0xFFDCE8FF)
val KhataOnPrimaryContainerLight = Color(0xFF0A2E5C)

val KhataSecondaryLight = Color(0xFF0FA35C) // Neon green, deepened for contrast on white
val KhataOnSecondaryLight = Color(0xFFFFFFFF)
val KhataSecondaryContainerLight = Color(0xFFD3F7E3)
val KhataOnSecondaryContainerLight = Color(0xFF07452A)

val KhataTertiaryLight = Color(0xFF0084B8)
val KhataOnTertiaryLight = Color(0xFFFFFFFF)
val KhataTertiaryContainerLight = Color(0xFFD3F0FB)
val KhataOnTertiaryContainerLight = Color(0xFF00374C)

val KhataErrorLight = Color(0xFFD32F2F)
val KhataOnErrorLight = Color(0xFFFFFFFF)
val KhataErrorContainerLight = Color(0xFFFFDAD6)
val KhataOnErrorContainerLight = Color(0xFF5C1A1A)

val KhataBackgroundLight = Color(0xFFF5F8FF)
val KhataOnBackgroundLight = Color(0xFF10151F)
val KhataSurfaceLight = Color(0xFFFFFFFF)
val KhataOnSurfaceLight = Color(0xFF10151F)
val KhataSurfaceVariantLight = Color(0xFFE6ECF7)
val KhataOnSurfaceVariantLight = Color(0xFF444F5E)
val KhataOutlineLight = Color(0xFFC0CBDA)

// Functional Ledger Accents — kept legible against both themes
val LedgerReceived = Color(0xFF17C777) // neon green success
val LedgerPending = Color(0xFFFFB020) // amber
val LedgerOverdue = Color(0xFFFF5A5A) // glowing red
val LedgerCardGradientStart = Color(0xFF1B5FD6) // blue
val LedgerCardGradientEnd = Color(0xFF17C777) // green
