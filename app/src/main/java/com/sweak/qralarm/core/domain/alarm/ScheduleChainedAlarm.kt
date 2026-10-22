package com.sweak.qralarm.core.domain.alarm

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Keeps an alarm's chain going: arms the chained alarm that has to ring next and stores its time
 * on that chained alarm, so the chain survives a restart.
 *
 * Only ever one chained alarm of a chain carries a next time - that is what marks the chain as
 * being in progress.
 */
class ScheduleChainedAlarm @Inject constructor(
    private val alarmsRepository: AlarmsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmNotifier: AlarmNotifier,
    private val setAlarm: SetAlarm,
    private val disableAlarm: DisableAlarm,
    private val clock: Clock
) {
    /**
     * @param isRescheduling true when an already armed chained alarm has to be armed again, for
     * example after a reboot. Otherwise the chained alarm that follows the one currently armed is
     * armed, or the first one when the parent alarm has just fired.
     */
    suspend operator fun invoke(alarmId: Long, isRescheduling: Boolean = false): Result {
        val alarm = alarmsRepository.getAlarm(alarmId = alarmId) ?: return Result.None
        val chainedAlarms = alarm.chainedAlarms

        val activeChainedAlarm = chainedAlarms.firstOrNull { it.isArmed }

        val chainedAlarmToArm = if (isRescheduling) {
            activeChainedAlarm
        } else if (activeChainedAlarm != null) {
            // Alarms later in the chain are the ones with a greater order:
            chainedAlarms
                .filter { it.order > activeChainedAlarm.order }
                .minByOrNull { it.order }
        } else {
            chainedAlarms.minByOrNull { it.order }
        }

        if (chainedAlarmToArm == null) {
            alarmsRepository.clearChainedAlarmsNextTimes(alarmId = alarmId)

            // The chain has run its course and the alarm id has a single pending intent, so the
            // parent alarm gets its own schedule back:
            restoreParentSchedule(alarm = alarm)

            return Result.None
        }

        val alarmTimeInMillis = if (isRescheduling) {
            activeChainedAlarm?.nextChainedAlarmTimeInMillis
        } else {
            ZonedDateTime.now(clock)
                .withSecond(0)
                .withNano(0)
                .plusMinutes(chainedAlarmToArm.delayMinutes.toLong())
                .toInstant()
                .toEpochMilli()
        } ?: return Result.None

        alarmScheduler.setAlarm(
            alarmId = alarmId,
            alarmTimeInMillis = alarmTimeInMillis,
            alarmType = AlarmType.CHAINED
        )

        chainedAlarms.forEach { chainedAlarm ->
            alarmsRepository.setChainedAlarmNextTime(
                chainedAlarmId = chainedAlarm.chainedAlarmId,
                nextTimeInMillis = if (chainedAlarm.chainedAlarmId ==
                    chainedAlarmToArm.chainedAlarmId
                ) {
                    alarmTimeInMillis
                } else null
            )
        }

        showChainedAlarmNotification(
            alarmId = alarmId,
            alarmTimeInMillis = alarmTimeInMillis
        )

        return Result.Chained(alarmTimeInMillis = alarmTimeInMillis)
    }

    private suspend fun restoreParentSchedule(alarm: Alarm) {
        if (alarm.repeatingMode is Alarm.RepeatingMode.Once) {
            disableAlarm(alarmId = alarm.alarmId)
        } else {
            setAlarm(alarmId = alarm.alarmId, isReschedulingMissedAlarm = false)
        }
    }

    private fun showChainedAlarmNotification(alarmId: Long, alarmTimeInMillis: Long) {
        val alarmDateTime = Instant.ofEpochMilli(alarmTimeInMillis)
            .atZone(ZoneId.systemDefault())

        alarmNotifier.showUpcomingAlarmNotification(
            alarmId = alarmId,
            alarmHourOfDay = alarmDateTime.hour,
            alarmMinute = alarmDateTime.minute,
            alarmType = AlarmType.CHAINED
        )
    }

    sealed class Result {
        data class Chained(val alarmTimeInMillis: Long) : Result()
        data object None : Result()
    }
}
