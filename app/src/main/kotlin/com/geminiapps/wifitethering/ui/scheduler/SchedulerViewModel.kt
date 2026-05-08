package com.geminiapps.wifitethering.ui.scheduler

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geminiapps.wifitethering.data.db.ScheduleDao
import com.geminiapps.wifitethering.data.model.Schedule
import com.geminiapps.wifitethering.data.model.ScheduleAction
import com.geminiapps.wifitethering.worker.ScheduleWorkerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val scheduleDao: ScheduleDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val schedules = scheduleDao.getAllSchedules().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /**
     * On API 31+, exact alarms require SCHEDULE_EXACT_ALARM permission.
     * If false, alarms will fire inexactly (up to ~15 min late).
     */
    val canScheduleExactAlarms: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }

    fun addSchedule(
        label: String,
        hourOfDay: Int,
        minute: Int,
        daysOfWeek: Int,
        action: ScheduleAction,
    ) {
        viewModelScope.launch {
            val id = scheduleDao.upsert(
                Schedule(
                    label = label,
                    hourOfDay = hourOfDay,
                    minute = minute,
                    daysOfWeek = daysOfWeek,
                    action = action,
                )
            )
            val saved = Schedule(
                id = id.toInt(),
                label = label,
                hourOfDay = hourOfDay,
                minute = minute,
                daysOfWeek = daysOfWeek,
                action = action,
            )
            ScheduleWorkerManager.schedule(context, saved)
        }
    }

    fun toggleSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updated = schedule.copy(enabled = !schedule.enabled)
            scheduleDao.update(updated)
            ScheduleWorkerManager.schedule(context, updated)
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleDao.delete(schedule)
            ScheduleWorkerManager.cancel(context, schedule)
        }
    }
}
