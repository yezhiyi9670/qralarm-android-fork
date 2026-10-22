package com.sweak.qralarm.features.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.text.format.DateFormat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.sweak.qralarm.R
import com.sweak.qralarm.alarm.ACTION_WIDGET_MIDNIGHT_UPDATE
import com.sweak.qralarm.alarm.WIDGET_MIDNIGHT_UPDATE_REQUEST_CODE
import com.sweak.qralarm.core.domain.alarm.Alarm
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.ChainedAlarm
import com.sweak.qralarm.core.ui.getDayString
import com.sweak.qralarm.core.ui.getTimeString
import com.sweak.qralarm.features.widget.QRAlarmWidget.Companion.BOTTOM_TEXT_KEY
import com.sweak.qralarm.features.widget.QRAlarmWidget.Companion.TIME_TEXT_KEY
import com.sweak.qralarm.features.widget.QRAlarmWidget.Companion.TOP_LABEL_KEY
import com.sweak.qralarm.features.widget.QRAlarmWidget.Companion.WIDGET_STATE_KEY
import com.sweak.qralarm.features.widget.QRAlarmWidget.WidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class QRAlarmWidgetUpdater @Inject constructor(
    private val alarmsRepository: AlarmsRepository,
    private val alarmManager: AlarmManager,
    @param:ApplicationContext private val appContext: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null
    private var observingJob: Job? = null
    private val debounceDelayMs = 1500L

    fun startObserving() {
        if (observingJob?.isActive == true) return

        observingJob = scope.launch {
            alarmsRepository.getAllAlarms()
                .drop(1) // the initial widget render is triggered by QRAlarmWidgetReceiver.onUpdate()
                .collect { requestUpdate() }
        }
    }

    fun requestUpdate() {
        // AppWidgetManager throws IllegalStateException before user unlock (Direct Boot).
        val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
        if (!userManager.isUserUnlocked) return

        updateJob?.cancel()
        updateJob = scope.launch {
            delay(debounceDelayMs.milliseconds)
            performUpdate()
        }
    }

    private suspend fun performUpdate() {
        val glanceIds = GlanceAppWidgetManager(appContext)
            .getGlanceIds(QRAlarmWidget::class.java)
        if (glanceIds.isEmpty()) return

        val widget = QRAlarmWidget()
        val resolved = resolveWidgetData()

        glanceIds.forEach { id ->
            updateAppWidgetState(appContext, id) { prefs ->
                prefs[WIDGET_STATE_KEY] = resolved.state.name
                prefs[TOP_LABEL_KEY] = resolved.topLabel
                prefs[TIME_TEXT_KEY] = resolved.timeText
                prefs[BOTTOM_TEXT_KEY] = resolved.bottomText
            }
            widget.update(appContext, id)
        }
    }

    private suspend fun resolveWidgetData(): ResolvedWidgetData {
        val upcomingAlarm = getUpcomingAlarm()
        val is24h = DateFormat.is24HourFormat(appContext)

        return when (upcomingAlarm) {
            null -> ResolvedWidgetData(
                state = WidgetState.NO_ALARM,
                topLabel = appContext.getString(R.string.no_alarms),
                timeText = NO_TIME_PLACEHOLDER,
                bottomText = appContext.getString(R.string.widget_tap_to_add)
            )

            is UpcomingAlarm.Snoozed -> {
                val timeMillis =
                    upcomingAlarm.alarm.snoozeConfig.nextSnoozedAlarmTimeInMillis ?: 0L
                ResolvedWidgetData(
                    state = WidgetState.SNOOZED,
                    topLabel = appContext.getString(R.string.snooze),
                    timeText = getTimeString(timeMillis, is24h),
                    bottomText = resolveDayString(timeMillis)
                )
            }

            is UpcomingAlarm.Chained -> {
                val timeMillis = upcomingAlarm.chainedAlarm.nextChainedAlarmTimeInMillis ?: 0L
                ResolvedWidgetData(
                    state = WidgetState.CHAINED,
                    topLabel = upcomingAlarm.chainedAlarm.label
                        ?: appContext.getString(R.string.alarms_chain),
                    timeText = getTimeString(timeMillis, is24h),
                    bottomText = resolveDayString(timeMillis)
                )
            }

            is UpcomingAlarm.Regular -> {
                val timeMillis = upcomingAlarm.alarm.nextAlarmTimeInMillis
                ResolvedWidgetData(
                    state = WidgetState.NORMAL,
                    topLabel = upcomingAlarm.alarm.alarmLabel
                        ?: appContext.getString(R.string.widget_next_alarm),
                    timeText = getTimeString(timeMillis, is24h),
                    bottomText = resolveDayString(timeMillis)
                )
            }
        }
    }

    private fun resolveDayString(timeMillis: Long): String {
        val alarmDate = Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return when (alarmDate) {
            today -> appContext.getString(R.string.widget_today)
            today.plusDays(1) -> appContext.getString(R.string.widget_tomorrow)
            else -> getDayString(timeMillis)
        }
    }

    /** Whichever of the three kinds of upcoming alarm comes first. */
    private suspend fun getUpcomingAlarm(): UpcomingAlarm? {
        val alarmsList = alarmsRepository.getAllAlarms().first()

        val nextRegularAlarm = alarmsList
            .filter { it.isAlarmEnabled }
            .minByOrNull { it.nextAlarmTimeInMillis }
            ?.let { UpcomingAlarm.Regular(alarm = it) }

        val nextSnoozedAlarm = alarmsList
            .filter {
                it.snoozeConfig.isAlarmSnoozed &&
                    it.snoozeConfig.nextSnoozedAlarmTimeInMillis != null
            }
            .minByOrNull { it.snoozeConfig.nextSnoozedAlarmTimeInMillis ?: Long.MAX_VALUE }
            ?.let { UpcomingAlarm.Snoozed(alarm = it) }

        val nextChainedAlarm = alarmsList
            .flatMap { alarm ->
                alarm.chainedAlarms
                    .filter { it.isArmed }
                    .map { chainedAlarm -> UpcomingAlarm.Chained(alarm, chainedAlarm) }
            }
            .minByOrNull { it.chainedAlarm.nextChainedAlarmTimeInMillis ?: Long.MAX_VALUE }

        return listOfNotNull(nextRegularAlarm, nextSnoozedAlarm, nextChainedAlarm)
            .minByOrNull { it.timeInMillis }
    }

    private sealed class UpcomingAlarm {
        abstract val timeInMillis: Long

        data class Regular(val alarm: Alarm) : UpcomingAlarm() {
            override val timeInMillis: Long
                get() = alarm.nextAlarmTimeInMillis
        }

        data class Snoozed(val alarm: Alarm) : UpcomingAlarm() {
            override val timeInMillis: Long
                get() = alarm.snoozeConfig.nextSnoozedAlarmTimeInMillis ?: Long.MAX_VALUE
        }

        data class Chained(val alarm: Alarm, val chainedAlarm: ChainedAlarm) : UpcomingAlarm() {
            override val timeInMillis: Long
                get() = chainedAlarm.nextChainedAlarmTimeInMillis ?: Long.MAX_VALUE
        }
    }

    private data class ResolvedWidgetData(
        val state: WidgetState,
        val topLabel: String,
        val timeText: String,
        val bottomText: String
    )

    fun scheduleMidnightWidgetUpdate() {
        val nextMidnightMillis = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(appContext, QRAlarmWidgetReceiver::class.java).apply {
            action = ACTION_WIDGET_MIDNIGHT_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            WIDGET_MIDNIGHT_UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, nextMidnightMillis, pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, nextMidnightMillis, pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, nextMidnightMillis, pendingIntent
            )
        }
    }

    companion object {
        private const val NO_TIME_PLACEHOLDER = "--:--"
    }
}
