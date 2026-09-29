package br.com.mestrecell.mestrescan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import br.com.mestrecell.mestrescan.R
import br.com.mestrecell.mestrescan.data.RiskLevel
import br.com.mestrecell.mestrescan.system.SystemActions
import br.com.mestrecell.mestrescan.ui.theme.RiskColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Moldura padrão das telas: barra de título + lista rolável. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenFrame(
    title: String?,
    onBack: (() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun AppIcon(packageName: String, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)))
    } else {
        Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(size))
    }
}

fun levelLabel(level: RiskLevel): String = when (level) {
    RiskLevel.DANGER -> "Perigoso"
    RiskLevel.SUSPECT -> "Suspeito"
    RiskLevel.SAFE -> "Normal"
}

@Composable
fun RiskBadge(level: RiskLevel, score: Int? = null) {
    val color = RiskColors.of(level)
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = if (score != null) "${levelLabel(level)} · $score" else levelLabel(level),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "Está com vírus de propaganda? O Mestre resolve." + botão de WhatsApp da loja. */
@Composable
fun WhatsAppCta(title: String = stringResource(R.string.brand_slogan)) {
    val context = LocalContext.current
    SectionCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Não conseguiu resolver? Traga na ${stringResource(R.string.brand_store)} ou chame no WhatsApp.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { SystemActions.openWhatsApp(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Falar com o Mestre no WhatsApp", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StoreFooter() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            "Feito pela ${stringResource(R.string.brand_store)} · ${stringResource(R.string.brand_tagline)}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(stringResource(R.string.brand_address), color = muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text(
            "${stringResource(R.string.brand_whatsapp_display)} · ${stringResource(R.string.brand_instagram)}",
            color = muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun AppRow(
    packageName: String,
    title: String,
    subtitle: String?,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** "1 app analisado" / "3 apps analisados". */
fun plural(count: Int, one: String, many: String): String = if (count == 1) "1 $one" else "$count $many"

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR")).format(Date(millis))

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(millis))
