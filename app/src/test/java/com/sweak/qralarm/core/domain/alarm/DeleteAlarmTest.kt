package com.sweak.qralarm.core.domain.alarm

import assertk.assertThat
import assertk.assertions.isNull
import com.sweak.qralarm.core.domain.fake.FakeAlarmsRepository
import com.sweak.qralarm.core.domain.fixture.alarm
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAlarmTest {

    private val alarmsRepository = spyk(FakeAlarmsRepository())
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)
    private val alarmRingtoneStorage = mockk<AlarmRingtoneStorage>(relaxUnitFun = true)
    private val deleteAlarm = DeleteAlarm(
        alarmsRepository = alarmsRepository,
        alarmScheduler = alarmScheduler,
        alarmRingtoneStorage = alarmRingtoneStorage
    )

    @Test
    fun `given stored alarm, when deleting, then the scheduled alarm is cancelled`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(alarm())

        // When
        deleteAlarm(alarmId)

        // Then
        verify { alarmScheduler.cancelAlarm(alarmId) }
    }

    @Test
    fun `given stored alarm, when deleting, then the alarm is removed`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(alarm())

        // When
        deleteAlarm(alarmId)

        // Then
        assertThat(alarmsRepository.getAlarm(alarmId)).isNull()
    }

    @Test
    fun `given stored alarm, when deleting, then its ringtone file is deleted`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(
            alarm(customRingtoneUriString = "ringtone-uri")
        )

        // When
        deleteAlarm(alarmId)

        // Then
        verify { alarmRingtoneStorage.deleteForAlarm(alarmId) }
    }
}
