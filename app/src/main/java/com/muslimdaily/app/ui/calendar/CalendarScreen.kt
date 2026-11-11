package com.muslimdaily.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muslimdaily.app.R
import com.muslimdaily.app.data.DayCell
import com.muslimdaily.app.data.dua.text
import com.muslimdaily.app.ui.common.localizeNumber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    vm: CalendarViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.titleLarge)
        }

        if (!ui.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthHeader(ui, locale, onPrev = vm::previousMonth, onNext = vm::nextMonth)
            WeekdayRow(locale)
            MonthGrid(ui.cells, ui.today, ui.selected, locale, onSelect = vm::select)
            AgendaSection(ui, locale, onOpenSurah)
        }
    }
}

@Composable
private fun MonthHeader(ui: CalendarUiState, locale: Locale, onPrev: () -> Unit, onNext: () -> Unit) {
    val months = stringArrayResource(R.array.hijri_months)
    val fmt = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val first = ui.cells.firstOrNull()?.date
    val last = ui.cells.lastOrNull()?.date
    val range = if (first == null || last == null) "" else {
        val a = first.format(fmt)
        val b = last.format(fmt)
        if (a == b) a else "$a – $b"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.prev_month))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${months[(ui.month - 1).coerceIn(0, 11)]} ${localizeNumber(ui.year, locale)}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
        }
    }
}

private val WEEK_ORDER: List<DayOfWeek> =
    listOf(DayOfWeek.SUNDAY) + DayOfWeek.values().filter { it != DayOfWeek.SUNDAY }

@Composable
private fun WeekdayRow(locale: Locale) {
    Row(Modifier.fillMaxWidth()) {
        WEEK_ORDER.forEach { d ->
            Text(
                d.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    cells: List<DayCell>,
    today: LocalDate,
    selected: LocalDate,
    locale: Locale,
    onSelect: (LocalDate) -> Unit,
) {
    if (cells.isEmpty()) return
    // Sunday-first: Sunday (7) -> 0, Monday (1) -> 1, ...
    val leading = cells.first().date.dayOfWeek.value % 7
    val slots: List<DayCell?> = List(leading) { null } + cells
    val padded = slots + List((7 - slots.size % 7) % 7) { null }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        padded.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f)) {
                        if (cell != null) DayCellView(cell, cell.date == today, cell.date == selected, locale) { onSelect(cell.date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCellView(cell: DayCell, isToday: Boolean, isSelected: Boolean, locale: Locale, onClick: () -> Unit) {
    val bg = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(localizeNumber(cell.hijriDay, locale), style = MaterialTheme.typography.titleMedium, color = fg)
        Text(localizeNumber(cell.date.dayOfMonth, locale), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
    }
}

@Composable
private fun AgendaSection(ui: CalendarUiState, locale: Locale, onOpenSurah: (Int) -> Unit) {
    val months = stringArrayResource(R.array.hijri_months)
    val timeFmt = remember(locale) { DateTimeFormatter.ofPattern("hh:mm a", locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val isBangla = locale.language == "bn"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(
            if (ui.selected == ui.today) stringResource(R.string.agenda_today) else ui.selected.format(dateFmt),
            style = MaterialTheme.typography.titleLarge,
        )
        ui.selectedHijri?.let { h ->
            Text(
                stringResource(
                    R.string.hijri_date,
                    localizeNumber(h.day, locale),
                    months[(h.month - 1).coerceIn(0, 11)],
                    localizeNumber(h.year, locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (ui.agenda.isEmpty()) {
            Text(stringResource(R.string.agenda_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        ui.agenda.forEach { entry ->
            val surah = entry.practice?.surah
            val rowModifier = if (surah != null) Modifier.clickable { onOpenSurah(surah) } else Modifier
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().then(rowModifier)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.time?.format(timeFmt) ?: "—",
                        modifier = Modifier.width(92.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (entry.prayer != null) stringResource(entry.prayer.nameRes) else entry.practice?.title.text(isBangla),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (entry.prayer != null) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (surah != null) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
