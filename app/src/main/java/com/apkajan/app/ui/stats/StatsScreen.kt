package com.apkajan.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkajan.app.R
import com.apkajan.app.data.apps.QuickApps
import com.apkajan.app.data.local.DayTotal
import com.apkajan.app.ui.components.BackTopBar
import com.apkajan.app.ui.components.SectionCard
import com.apkajan.app.ui.theme.Danger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val Palette = listOf(
    Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFF59E0B),
    Color(0xFF8B5CF6), Color(0xFF0EA5E9), Color(0xFF84CC16)
)

private fun colorFor(pkg: String, index: Int): Color =
    QuickApps.forPackage(pkg)?.let { Color(it.color) } ?: Palette[index % Palette.size]

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
    val weekly by vm.weekly.collectAsStateWithLifecycle()
    val breakdown by vm.breakdown.collectAsStateWithLifecycle()
    val selected by vm.selectedDate.collectAsStateWithLifecycle()
    val limit by vm.limit.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    val today = weekly.lastOrNull()?.total ?: 0
    val weekTotal = weekly.sumOf { it.total }
    val avg = if (weekly.isNotEmpty()) weekTotal / weekly.size else 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(R.string.statistics), onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Özet kutucukları
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.stats_today_total), today, Modifier.weight(1f),
                    highlight = today >= limit)
                StatTile(stringResource(R.string.stats_week_total), weekTotal, Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_daily_avg), avg, Modifier.weight(1f))
            }

            SectionCard(title = stringResource(R.string.stats_last7)) {
                WeeklyBarChart(
                    days = weekly,
                    selectedDate = selected,
                    limit = limit,
                    locale = locale,
                    onSelect = vm::select
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.stats_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val dateLabel = LocalDate.parse(selected)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            SectionCard(title = stringResource(R.string.stats_by_app, dateLabel)) {
                if (breakdown.isEmpty()) {
                    Text(
                        stringResource(R.string.stats_no_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val dayTotal = breakdown.sumOf { it.count }
                    Text(
                        stringResource(R.string.stats_day_total, dayTotal),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    // Yığılmış dağılım çubuğu
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                    ) {
                        breakdown.forEachIndexed { i, s ->
                            Box(
                                Modifier
                                    .weight(s.count.toFloat().coerceAtLeast(0.0001f))
                                    .fillMaxHeight()
                                    .background(colorFor(s.packageName, i))
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    breakdown.forEachIndexed { i, s ->
                        val color = colorFor(s.packageName, i)
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(color, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(s.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text("${s.count}", fontWeight = FontWeight.Bold)
                                Text(
                                    "  ${(s.fraction * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { s.fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        Text(
            "$value",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = if (highlight) Danger else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Son 7 günün sütun grafiği. Sütuna dokununca o gün seçilir; kesikli çizgi günlük limiti gösterir.
 * Limit aşılan günler kırmızı, seçili gün ana renkte, diğerleri soluk.
 */
@Composable
fun WeeklyBarChart(
    days: List<DayTotal>,
    selectedDate: String,
    limit: Int,
    locale: Locale,
    onSelect: (String) -> Unit,
    barAreaHeight: Dp = 150.dp
) {
    val maxVal = maxOf(days.maxOfOrNull { it.total } ?: 0, limit, 1)
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val lineColor = Danger.copy(alpha = 0.7f)
    val limitLabel = stringResource(R.string.stats_limit_line)

    Column {
        Box(Modifier.fillMaxWidth().height(barAreaHeight + 22.dp)) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { d ->
                    val isSel = d.date == selectedDate
                    val over = d.total >= limit && d.total > 0
                    val barColor = when {
                        over && isSel -> Danger
                        over -> Danger.copy(alpha = 0.5f)
                        isSel -> primary
                        else -> muted
                    }
                    val h = barAreaHeight * (d.total.toFloat() / maxVal)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(d.date) }
                            .semantics { contentDescription = "${d.date}: ${d.total}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            "${d.total}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height(if (d.total > 0) h.coerceAtLeast(3.dp) else 2.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor)
                        )
                    }
                }
            }
            // Limit çizgisi
            if (limit > 0) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height - barAreaHeight.toPx() * (limit.toFloat() / maxVal)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }
                Text(
                    "$limitLabel $limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d ->
                val isSel = d.date == selectedDate
                Text(
                    LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
