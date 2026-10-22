package com.sweak.qralarm.core.domain.alarm

import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

class SnoozeAlarm @Inject constructor(
    private val alarmScheduler: AlarmScheduler,
    private val alarmNotifier: AlarmNotifier,
    private val alarmsRepository: AlarmsRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(alarmId: Long, isReschedulingCurrentOrMissedSnooze: Boolean) {
        val alarm = alarmsRepository.getAlarm(alarmId = alarmId) ?: return
        val snoozeAlarmTimeInMillis =
            if (isReschedulingCurrentOrMissedSnooze) alarm.snoozeConfig.nextSnoozedAlarmTimeInMillis
            else ZonedDateTime.now(clock)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(alarm.snoozeConfig.snoozeMode.snoozeDurationInMinutes.toLong())
                .toInstant()
                .toEpochMilli()

        alarmsRepository.addOrEditAlarm(
            chainedAlarms = alarm.chainedAlarms,
            alarm = alarm.copy(
                snoozeConfig = alarm.snoozeConfig.copy(
                    numberOfSnoozesLeft =
                        if (isReschedulingCurrentOrMissedSnooze) alarm.snoozeConfig.numberOfSnoozesLeft
                        else (alarm.snoozeConfig.numberOfSnoozesLeft - 1).coerceAtLeast(0),
                    isAlarmSnoozed = true,
                    nextSnoozedAlarmTimeInMillis = snoozeAlarmTimeInMillis
                )
            )
        )

        alarmsRepository.clearChainedAlarmsNextTimes(alarmId = alarm.alarmId)

        if (snoozeAlarmTimeInMillis != null) {
            alarmScheduler.setAlarm(
                alarmId = alarm.alarmId,
                alarmTimeInMillis = snoozeAlarmTimeInMillis,
                alarmType = AlarmType.SNOOZED
            )

            val (alarmHourOfDay, alarmMinute) =
                getHourAndMinuteOfAlarmTimeInMillis(snoozeAlarmTimeInMillis, clock.zone)

            alarmNotifier.showUpcomingAlarmNotification(
                alarmId = alarm.alarmId,
                alarmHourOfDay = alarmHourOfDay,
                alarmMinute = alarmMinute,
                alarmType = AlarmType.SNOOZED
            )
        }
    }
}
