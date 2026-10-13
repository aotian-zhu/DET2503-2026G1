package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.dte2503.volunteer.data.TaskStatus
import no.dte2503.volunteer.ui.theme.*

@Composable
fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

@Composable
fun InfoCard(highlighted: Boolean = false, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable
fun ArcticHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(ArcticNavy, Color(0xFF144CA5))))) {
        Canvas(Modifier.matchParentSize()) {
            val ridge = Path().apply {
                moveTo(size.width * .34f, size.height); lineTo(size.width * .73f, size.height * .30f)
                lineTo(size.width * .88f, size.height * .58f); lineTo(size.width, size.height * .41f)
                lineTo(size.width, size.height); close()
            }
            drawPath(ridge, Color(0xFF67B7F2).copy(alpha = .16f))
            val foreground = Path().apply {
                moveTo(size.width * .53f, size.height); lineTo(size.width * .87f, size.height * .57f)
                lineTo(size.width, size.height * .79f); lineTo(size.width, size.height); close()
            }
            drawPath(foreground, Glacier.copy(alpha = .16f))
        }
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun StatusPill(status: TaskStatus) {
    val color = if (status == TaskStatus.DONE) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val text = when (status) { TaskStatus.TODO -> "To do"; TaskStatus.IN_PROGRESS -> "In progress"; TaskStatus.DONE -> "Completed" }
    Surface(shape = RoundedCornerShape(8.dp), color = color) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall) }
}
