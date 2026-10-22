package com.sweak.qralarm.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sweak.qralarm.alarm.QRAlarmManager
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.AlarmType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PostUpcomingAlarmNotificationReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var alarmsRepository: AlarmsRepository
    @Inject
    lateinit var qrAlarmManager: QRAlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val alarmId = intent.extras?.getLong(EXTRA_ALARM_ID) ?: return@launch

                if (alarmId == 0L) return@launch

                val alarm = alarmsRepository.getAlarm(alarmId = alarmId) ?: return@launch

                val activeChainedAlarm = alarm.chainedAlarms.firstOrNull {
                    it.nextChainedAlarmTimeInMillis != null
                }
                val alarmType = when {
                    activeChainedAlarm != null -> AlarmType.CHAINED
                    alarm.snoozeConfig.isAlarmSnoozed -> AlarmType.SNOOZED
                    else -> AlarmType.REGULAR
                }

                val chainedAlarmDateTime = activeChainedAlarm
                    ?.nextChainedAlarmTimeInMillis
                    ?.let { timeInMillis ->
                        java.time.Instant.ofEpochMilli(timeInMillis)
                            .atZone(java.time.ZoneId.systemDefault())
                    }

                qrAlarmManager.showUpcomingAlarmNotification(
                    alarmId = alarm.alarmId,
                    alarmHourOfDay = chainedAlarmDateTime?.hour ?: alarm.alarmHourOfDay,
                    alarmMinute = chainedAlarmDateTime?.minute ?: alarm.alarmMinute,
                    alarmType = alarmType
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarmId"
    }
}