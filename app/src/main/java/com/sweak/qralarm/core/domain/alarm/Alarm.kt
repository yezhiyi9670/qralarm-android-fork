package com.sweak.qralarm.core.domain.alarm

import java.time.DayOfWeek

data class Alarm(
    val alarmId: Long,
    val alarmHourOfDay: Int,
    val alarmMinute: Int,
    val isAlarmEnabled: Boolean,
    val isAlarmRunning: Boolean,
    val repeatingMode: RepeatingMode,
    val nextAlarmTimeInMillis: Long,
    val snoozeConfig: SnoozeConfig,
    val ringtone: Ringtone,
    val customRingtoneUriString: String?,
    /** The file name the custom ringtone had when it was picked, shown to the user. */
    val customRingtoneName: String? = null,
    val alarmVolumeMode: AlarmVolumeMode,
    val areVibrationsEnabled: Boolean,
    val isUsingCode: Boolean,
    val assignedCode: Code?,
    val isOpenCodeLinkEnabled: Boolean,
    val cancelLockDurationInMinutes: Int,
    val isEmergencyTaskEnabled: Boolean,
    val alarmLabel: String?,
    val gentleWakeUpDurationInSeconds: Int,
    val temporaryMuteDurationInSeconds: Int,
    val skipAlarmUntilTimeInMillis: Long?,
    val chainedAlarms: List<ChainedAlarm> = emptyList()
) {
    sealed class RepeatingMode {
        data object Once : RepeatingMode()
        data class Days(val repeatingDaysOfWeek: List<DayOfWeek>) : RepeatingMode()
    }

    data class SnoozeConfig(
        val snoozeMode: SnoozeMode,
        val numberOfSnoozesLeft: Int,
        val isAlarmSnoozed: Boolean,
        val nextSnoozedAlarmTimeInMillis: Long?
    )

    data class SnoozeMode(
        val numberOfSnoozes: Int,
        val snoozeDurationInMinutes: Int
    )

    enum class Ringtone {
        GENTLE_GUITAR,
        KALIMBA,
        CLASSIC_ALARM,
        ALARM_CLOCK,
        ROOSTER,
        AIR_HORN,
        SYSTEM_DEFAULT,
        CUSTOM_SOUND
    }

    sealed class AlarmVolumeMode {
        data object System : AlarmVolumeMode()
        data class Custom(val volumePercentage: Int) : AlarmVolumeMode()
    }
}
