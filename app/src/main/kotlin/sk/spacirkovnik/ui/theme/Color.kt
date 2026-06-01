package sk.spacirkovnik.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

val ForestMid = Color(0xFF3A6B5A)
val ForestLight = Color(0xFF4E8E7A)

val Amber = Color(0xFFD4933E)
val AmberLight = Color(0xFFE8B86D)

val TextDark = Color(0xFF1C2B25)
val TextMedium = Color(0xFF4A635A)
val TextOnDark = Color(0xFFF5F9F7)

val CardBg = Color(0xFFFAF6F1)

val PrimaryButton = Color(0xFFC9761B)       // primárne tlačidlo (Hrať, Stiahnuť...)
val PrimaryButtonText = Color(0xFFFFFFFF)
val SecondaryButton = Color(0xFF3A6B5A)     // sekundárne plné tlačidlo (kvízové odpovede v hre)
val SecondaryOutlineButton = Color(0xFF7A4A22) // sekundárne obrysové tlačidlo (Aktualizovať)
val PurchaseButton = Color(0xFF43A047)
val SecondaryButtonText = Color(0xFFFFFFFF)
val BackButton = Color(0x40FFFFFF)
val BackButtonText = Color(0xFFFFFFFF)
val DisabledButton = Color(0x30FFFFFF)

val GameBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1C3A2E),
        Color(0xFF2D4A3E),
        Color(0xFF3A6B5A),
        Color(0xFF4E8E7A),
    )
)

val MainBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF0E9DF),
        Color(0xFFE8D9C8),
        Color(0xFFDDC9B4),
    )
)

// Text farby pre svetlé pozadie (zoznam hier)
val TextOnBeige = Color(0xFF3D2314)
val TextOnBeigeSecondary = Color(0xFF6B4C35)

fun gameGradient(colorHex: String?): Brush {
    if (colorHex == null) return GameBackground
    return try {
        val base = "#$colorHex".toColorInt()
        val r = (base shr 16 and 0xFF) / 255f
        val g = (base shr 8 and 0xFF) / 255f
        val b = (base and 0xFF) / 255f
        Brush.verticalGradient(
            colors = listOf(
                Color(r * 0.7f, g * 0.7f, b * 0.7f),
                Color(r * 0.85f, g * 0.85f, b * 0.85f),
                Color(r, g, b)
            )
        )
    } catch (_: Exception) {
        GameBackground
    }
}

