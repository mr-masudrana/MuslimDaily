package com.muslimdaily.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muslimdaily.app.data.AgendaRepository
import com.muslimdaily.app.data.DayCell
import com.muslimdaily.app.data.HijriUtil
import com.muslimdaily.app.data.PrayerRepository
import com.muslimdaily.app.data.SettingsStore
import com.muslimdaily.app.data.agenda.Practice
import com.muslimdaily.app.data.agenda.matches
import com.muslimdaily.app.data.agenda.timeOrNull
import com.muslimdaily.app.data.model.HijriDate
import com.muslimdaily.app.data.model.Prayer
import com.muslimdaily.app.data.model.PrayerDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class AgendaEntry(val time: LocalTime?, val prayer: Prayer?, val practice: Practice?)

data class CalendarUiState(
    val ready: Boolean = false,
    val year: Int = 0,
    val month: Int = 0,
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val selectedHijri: HijriDate? = null,
    val cells: List<DayCell> = emptyList(),
    val agenda: List<AgendaEntry> = emptyList(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val prayers: PrayerRepository,
    private val agendaRepo: AgendaRepository,
) : ViewModel() {

    private val month = MutableStateFlow<Pair<Int, Int>?>(null) // (hijri year, hijri month)
    private val selected = MutableStateFlow(LocalDate.now())
    private val practices = MutableStateFlow<List<Practice>>(emptyList())
    private val prayerDay = MutableStateFlow<PrayerDay?>(null)

    val ui: StateFlow<CalendarUiState> = combine(
        settings.hijriOffset, month, selected, practices, prayerDay
    ) { offset, m, sel, pr, pd ->
        if (m == null) {
            CalendarUiState()
        } else {
            val today = LocalDate.now()
            val hijriSel = HijriUtil.of(sel, offset)
            CalendarUiState(
                ready = true,
                year = m.first,
                month = m.second,
                today = today,
                selected = sel,
                selectedHijri = hijriSel,
                cells = HijriUtil.monthCells(m.first, m.second, offset),
                agenda = buildAgenda(sel, today, hijriSel, pr, pd),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        viewModelScope.launch {
            practices.value = agendaRepo.practices()
            prayerDay.value = prayers.cachedToday()
            val h = HijriUtil.of(LocalDate.now(), settings.hijriOffset.first())
            month.value = h.year to h.month
        }
    }

    fun select(date: LocalDate) { selected.value = date }

    fun previousMonth() = moveMonth(-1)
    fun nextMonth() = moveMonth(1)

    private fun moveMonth(delta: Int) {
        val (y, m) = month.value ?: return
        var year = y
        var mon = m + delta
        if (mon < 1) { mon = 12; year-- }
        if (mon > 12) { mon = 1; year++ }
        if (year in HijriUtil.MIN_YEAR..HijriUtil.MAX_YEAR) month.value = year to mon
    }

    private fun buildAgenda(
        sel: LocalDate,
        today: LocalDate,
        hijri: HijriDate,
        practices: List<Practice>,
        pd: PrayerDay?,
    ): List<AgendaEntry> {
        val list = mutableListOf<AgendaEntry>()
        // Prayer times are only known for today (cached from the Home screen).
        if (sel == today && pd != null && pd.date == today) {
            pd.times.forEach { (prayer, time) -> list += AgendaEntry(time, prayer, null) }
        }
        practices.filter { it.matches(sel, hijri) }.forEach { list += AgendaEntry(it.timeOrNull(), null, it) }
        return list.sortedBy { it.time?.toSecondOfDay() ?: Int.MAX_VALUE }
    }
}
