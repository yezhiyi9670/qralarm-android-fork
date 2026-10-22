package com.sweak.qralarm.core.data.backup

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.sweak.qralarm.core.data.backup.dto.AlarmDto
import com.sweak.qralarm.core.storage.database.model.AlarmEntity
import com.sweak.qralarm.core.storage.database.model.ChainedAlarmEntity
import org.junit.Test

class BackupChainedAlarmMappingTest {

    @Test
    fun `given an alarm with a chain, when exporting and importing, then the chain is kept`() {
        // Given
        val alarmEntity = alarmEntity()

        // When
        val backupAlarm = alarmEntity.toBackupAlarm(
            chainedAlarms = listOf(
                chainedAlarmEntity(delayMinutes = 5, order = 0, label = "Bathroom"),
                chainedAlarmEntity(
                    delayMinutes = 15,
                    order = 1,
                    assignedCodeId = 3,
                    label = null
                )
            )
        )
        val restored = backupAlarm.toAlarmDto().toBackupAlarm()

        // Then
        assertThat(restored.chainedAlarms.map { it.delayMinutes }).isEqualTo(listOf(5, 15))
        assertThat(restored.chainedAlarms.map { it.label })
            .isEqualTo(listOf("Bathroom", null))
        assertThat(restored.chainedAlarms.map { it.assignedCodeId }).isEqualTo(listOf(null, 3L))
        assertThat(restored.chainedAlarms.map { it.order }).isEqualTo(listOf(0, 1))
    }

    @Test
    fun `given a backup from before chains existed, when importing, then the alarm has no chain`() {
        // Given
        val dtoWithoutChains = AlarmDto(
            alarmId = 1,
            alarmHourOfDay = 7,
            alarmMinute = 30,
            nextAlarmTimeInMillis = 0,
            repeatingAlarmDays = null,
            numberOfSnoozes = 3,
            snoozeDurationInMinutes = 10,
            ringtone = "GENTLE_GUITAR",
            hasCustomRingtoneFile = false,
            alarmVolumePercentage = 0,
            areVibrationsEnabled = true,
            isUsingCode = false,
            assignedCodeId = null,
            isOpenCodeLinkEnabled = false,
            cancelLockDurationInMinutes = 0,
            isEmergencyTaskEnabled = false,
            alarmLabel = null,
            gentleWakeUpDurationInSeconds = 0,
            temporaryMuteDurationInSeconds = 0
        )

        // When
        val restored = dtoWithoutChains.toBackupAlarm()

        // Then
        assertThat(restored.chainedAlarms).isEqualTo(emptyList())
    }

    @Test
    fun `given a restored alarm, when it is written to the database, then no chained alarm time is armed`() {
        // When
        val alarmEntity = backupAlarm().toAlarmEntity(assignedCodeId = null)

        // Then
        assertThat(alarmEntity.alarmId).isEqualTo(0)
        assertThat(alarmEntity.isAlarmEnabled).isEqualTo(false)
        assertThat(alarmEntity.isAlarmRunning).isEqualTo(false)
        assertThat(alarmEntity.nextSnoozedAlarmTimeInMillis).isNull()
        assertThat(alarmEntity.skipAlarmUntilTimeInMillis).isNull()
    }

    private fun backupAlarm() = AlarmDto(
        alarmId = 1,
        alarmHourOfDay = 7,
        alarmMinute = 30,
        nextAlarmTimeInMillis = 0,
        repeatingAlarmDays = null,
        numberOfSnoozes = 3,
        snoozeDurationInMinutes = 10,
        ringtone = "GENTLE_GUITAR",
        hasCustomRingtoneFile = false,
        alarmVolumePercentage = 0,
        areVibrationsEnabled = true,
        isUsingCode = false,
        assignedCodeId = null,
        isOpenCodeLinkEnabled = false,
        cancelLockDurationInMinutes = 0,
        isEmergencyTaskEnabled = false,
        alarmLabel = null,
        gentleWakeUpDurationInSeconds = 0,
        temporaryMuteDurationInSeconds = 0
    ).toBackupAlarm()

    private fun alarmEntity() = AlarmEntity(
        alarmId = 1,
        alarmHourOfDay = 7,
        alarmMinute = 30,
        isAlarmEnabled = true,
        isAlarmRunning = false,
        nextAlarmTimeInMillis = 0,
        repeatingAlarmDays = null,
        numberOfSnoozes = 3,
        snoozeDurationInMinutes = 10,
        numberOfSnoozesLeft = 3,
        isAlarmSnoozed = false,
        nextSnoozedAlarmTimeInMillis = null,
        ringtone = "GENTLE_GUITAR",
        customRingtoneUriString = null,
        alarmVolumePercentage = 0,
        areVibrationsEnabled = true,
        isUsingCode = false,
        assignedCodeId = null,
        isOpenCodeLinkEnabled = false,
        cancelLockDurationInMinutes = 0,
        isEmergencyTaskEnabled = false,
        alarmLabel = null,
        gentleWakeUpDurationInSeconds = 0,
        temporaryMuteDurationInSeconds = 0,
        skipAlarmUntilTimeInMillis = null
    )

    private fun chainedAlarmEntity(
        delayMinutes: Int,
        order: Int,
        assignedCodeId: Long? = null,
        label: String? = null
    ) = ChainedAlarmEntity(
        chainedAlarmId = order.toLong() + 1,
        parentAlarmId = 1,
        delayMinutes = delayMinutes,
        assignedCodeId = assignedCodeId,
        label = label,
        order = order,
        nextChainedAlarmTimeInMillis = null
    )
}
