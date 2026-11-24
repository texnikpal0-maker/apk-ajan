package com.apkajan.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.apkajan.app.AppContainer
import com.apkajan.app.ApkAjanApp
import com.apkajan.app.core.DayKey
import com.apkajan.app.data.local.AppDailyCount
import com.apkajan.app.data.local.DayTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Seçili gün için bir uygulamanın payı (UI'da etiket ve renk eklenir). */
data class AppShare(val packageName: String, val label: String, val count: Int, val fraction: Float)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(private val c: AppContainer) : ViewModel() {

    /** Son 7 gün, eskiden yeniye (bugün en sağda). */
    val days: List<String> = (6 downTo 0).map { DayKey.daysAgo(it.toLong()) }

    private val _selectedDate = MutableStateFlow(days.last())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val limit = c.settings.limit

    /** Veri olmayan günler 0 ile doldurulur → grafikte her zaman 7 sütun. */
    val weekly: StateFlow<List<DayTotal>> = c.usageDao.observeDailyTotals(days.first())
        .map { list ->
            val byDate = list.associate { it.date to it.total }
            days.map { DayTotal(it, byDate[it] ?: 0) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), days.map { DayTotal(it, 0) })

    val breakdown: StateFlow<List<AppShare>> = _selectedDate
        .flatMapLatest { date -> c.usageDao.observeDay(date) }
        .map { rows -> toShares(rows) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(date: String) {
        _selectedDate.value = date
    }

    private fun toShares(rows: List<AppDailyCount>): List<AppShare> {
        // TikTok'un farklı paket adları vb. aynı etiket altında birleştirilir.
        val total = rows.sumOf { it.count }.coerceAtLeast(1)
        return rows
            .groupBy { c.installedApps.label(it.packageName) }
            .map { (label, list) ->
                val cnt = list.sumOf { it.count }
                AppShare(list.first().packageName, label, cnt, cnt.toFloat() / total)
            }
            .sortedByDescending { it.count }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel((this[APPLICATION_KEY] as ApkAjanApp).container) }
        }
    }
}
