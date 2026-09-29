package br.com.mestrecell.mestrescan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import br.com.mestrecell.mestrescan.R
import br.com.mestrecell.mestrescan.data.RiskLevel

object RiskColors {
    val danger = Color(0xFFEF4444)
    val suspect = Color(0xFFF0C24B)
    val safe = Color(0xFF22C55E)

    fun of(level: RiskLevel): Color = when (level) {
        RiskLevel.DANGER -> danger
        RiskLevel.SUSPECT -> suspect
        RiskLevel.SAFE -> safe
    }
}

/** Cores vêm dos recursos do sabor (marca), então cada assistência tem o seu tema. */
@Composable
fun MestreScanTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = colorResource(R.color.brand_primary),
        onPrimary = colorResource(R.color.brand_on_primary),
        background = colorResource(R.color.brand_background),
        onBackground = colorResource(R.color.brand_text),
        surface = colorResource(R.color.brand_background),
        onSurface = colorResource(R.color.brand_text),
        surfaceVariant = colorResource(R.color.brand_surface),
        onSurfaceVariant = colorResource(R.color.brand_text_muted),
        surfaceContainer = colorResource(R.color.brand_surface),
        outline = colorResource(R.color.brand_border),
        outlineVariant = colorResource(R.color.brand_border),
        error = RiskColors.danger,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
