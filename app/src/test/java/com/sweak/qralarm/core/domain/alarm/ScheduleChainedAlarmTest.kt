package com.sweak.qralarm.core.domain.alarm

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sweak.qralarm.core.domain.fake.FakeAlarmsRepository
import com.sweak.qralarm.core.domain.fixture.alarm
import com.sweak.qralarm.core.domain.fixture.chainedAlarm
import com.sweak.qralarm.core.domain.fixture.epochMillisAt
import com.sweak.qralarm.core.domain.fixture.fixedClock
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class ScheduleChainedAlarmTest {

    private val alarmsRepository = FakeAlarmsRepository()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxUnitFun = true)
    private val alarmNotifier = mockk<AlarmNotifier>(relaxUnitFun = true)

    private fun scheduleChainedAlarm(now: LocalDateTime): ScheduleChainedAlarm {
        val clock = fixedClock(now)

        return ScheduleChainedAlarm(
            alarmsRepository = alarmsRepository,
            alarmScheduler = alarmScheduler,
            alarmNotifier = alarmNotifier,
            setAlarm = SetAlarm(
                alarmScheduler = alarmScheduler,
                alarmNotifier = alarmNotifier,
                alarmsRepository = alarmsRepository,
                clock = clock
            ),
            disableAlarm = DisableAlarm(
                alarmScheduler = alarmScheduler,
                alarmsRepository = alarmsRepository
            ),
            clock = clock
        )
    }

    @Test
    fun `given a chain, when the parent alarm fires, then the first chained alarm is armed after its delay`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(
            alarm = alarm(),
            chainedAlarms = listOf(
                chainedAlarm(delayMinutes = 5, order = 0),
                chainedAlarm(delayMinutes = 10, order = 1)
            )
        )

        // When
        scheduleChainedAlarm(now = NOW)(alarmId = alarmId)

        // Then
        val chainedAlarms = alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId)

        assertThat(chainedAlarms[0].nextChainedAlarmTimeInMillis)
            .isEqualTo(epochMillisAt(NOW.plusMinutes(5)))
        assertThat(chainedAlarms[1].nextChainedAlarmTimeInMillis).isEqualTo(null)
        verify {
            alarmScheduler.setAlarm(
                alarmId = alarmId,
                alarmTimeInMillis = epochMillisAt(NOW.plusMinutes(5)),
                alarmType = AlarmType.CHAINED
            )
        }
    }

    @Test
    fun `given a chain, when a chained alarm fires, then the next one is armed with its own delay`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(
            alarm = alarm(),
            chainedAlarms = listOf(
                chainedAlarm(delayMinutes = 5, order = 0),
                chainedAlarm(delayMinutes = 15, order = 1)
            )
        )
        val firstChainedAlarm = alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId)[0]
        alarmsRepository.setChainedAlarmNextTime(
            chainedAlarmId = firstChainedAlarm.chainedAlarmId,
            nextTimeInMillis = epochMillisAt(NOW)
        )

        // When
        scheduleChainedAlarm(now = NOW)(alarmId = alarmId)

        // Then
        val chainedAlarms = alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId)

        assertThat(chainedAlarms[0].nextChainedAlarmTimeInMillis).isEqualTo(null)
        assertThat(chainedAlarms[1].nextChainedAlarmTimeInMillis)
            .isEqualTo(epochMillisAt(NOW.plusMinutes(15)))
    }

    @Test
    fun `given a chain, when its last alarm fires, then no chained alarm stays armed`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(
            alarm = alarm(),
            chainedAlarms = listOf(chainedAlarm(delayMinutes = 5, order = 0))
        )
        val onlyChainedAlarm = alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId)[0]
        alarmsRepository.setChainedAlarmNextTime(
            chainedAlarmId = onlyChainedAlarm.chainedAlarmId,
            nextTimeInMillis = epochMillisAt(NOW)
        )

        // When
        scheduleChainedAlarm(now = NOW)(alarmId = alarmId)

        // Then
        assertThat(
            alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId).map {
                it.nextChainedAlarmTimeInMillis
            }
        ).isEqualTo(listOf(null))
    }

    @Test
    fun `given a stored chained alarm time, when rescheduling, then the stored time is rearmed`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(
            alarm = alarm(),
            chainedAlarms = listOf(
                chainedAlarm(delayMinutes = 5, order = 0),
                chainedAlarm(delayMinutes = 20, order = 1)
            )
        )
        val chainedAlarms = alarmsRepository.getChainedAlarmsForAlarm(alarmId = alarmId)
        alarmsRepository.setChainedAlarmNextTime(
            chainedAlarmId = chainedAlarms[1].chainedAlarmId,
            nextTimeInMillis = epochMillisAt(NOW.plusMinutes(20))
        )

        // When
        scheduleChainedAlarm(now = NOW)(alarmId = alarmId, isRescheduling = true)

        // Then
        verify {
            alarmScheduler.setAlarm(
                alarmId = alarmId,
                alarmTimeInMillis = epochMillisAt(NOW.plusMinutes(20)),
                alarmType = AlarmType.CHAINED
            )
        }
    }

    @Test
    fun `given an alarm without a chain, when scheduling a chained alarm, then nothing is scheduled`() = runTest {
        // Given
        val alarmId = alarmsRepository.addOrEditAlarm(alarm = alarm())

        // When
        val result = scheduleChainedAlarm(now = NOW)(alarmId = alarmId)

        // Then
        assertThat(result).isEqualTo(ScheduleChainedAlarm.Result.None)
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2025, 1, 4, 7, 0)
    }
}
