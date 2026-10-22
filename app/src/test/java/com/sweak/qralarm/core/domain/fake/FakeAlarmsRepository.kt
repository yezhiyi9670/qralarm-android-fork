package com.sweak.qralarm.core.domain.fake

import com.sweak.qralarm.core.domain.alarm.Alarm
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.ChainedAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAlarmsRepository : AlarmsRepository {

    private val alarms = mutableMapOf<Long, Alarm>()
    private var lastAlarmId = 0L

    suspend fun addOrEditAlarm(alarm: Alarm): Long = addOrEditAlarm(alarm, emptyList())

    override suspend fun addOrEditAlarm(
        alarm: Alarm,
        chainedAlarms: List<ChainedAlarm>
    ): Long {
        val alarmId = if (alarm.alarmId == 0L) lastAlarmId + 1 else alarm.alarmId
        lastAlarmId = maxOf(lastAlarmId, alarmId)

        alarms[alarmId] = alarm.copy(
            alarmId = alarmId,
            chainedAlarms = chainedAlarms.mapIndexed { index, chainedAlarm ->
                chainedAlarm.copy(
                    chainedAlarmId = if (chainedAlarm.chainedAlarmId == 0L) {
                        index.toLong() + 1
                    } else chainedAlarm.chainedAlarmId,
                    order = index
                )
            }
        )

        return alarmId
    }

    override suspend fun getAlarm(alarmId: Long): Alarm? = alarms[alarmId]

    override suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean) {
        alarms.computeIfPresent(alarmId) { _, alarm -> alarm.copy(isAlarmEnabled = enabled) }
    }

    override suspend fun setAlarmRunning(alarmId: Long, running: Boolean) = TODO()

    override suspend fun setAlarmSnoozed(alarmId: Long, snoozed: Boolean) {
        alarms.computeIfPresent(alarmId) { _, alarm ->
            alarm.copy(
                snoozeConfig = alarm.snoozeConfig.copy(
                    isAlarmSnoozed = snoozed,
                    nextSnoozedAlarmTimeInMillis =
                        if (!snoozed) null else alarm.snoozeConfig.nextSnoozedAlarmTimeInMillis
                )
            )
        }
    }

    override suspend fun setSkipNextAlarm(alarmId: Long, skip: Boolean) = TODO()
    override fun getAlarmFlow(alarmId: Long): Flow<Alarm> = TODO()
    override fun getAllAlarms(): Flow<List<Alarm>> = flow { emit(alarms.values.toList()) }
    override suspend fun deleteAlarm(alarmId: Long) {
        alarms.remove(alarmId)
    }

    override suspend fun setAvailableSnoozes(alarmId: Long, availableSnoozes: Int) = TODO()
    override suspend fun setAlarmRingtoneUri(alarmId: Long, uri: String?) {
        alarms.computeIfPresent(alarmId) { _, alarm -> alarm.copy(customRingtoneUriString = uri) }
    }

    override suspend fun setAlarmRingtone(alarmId: Long, ringtone: Alarm.Ringtone) {
        alarms.computeIfPresent(alarmId) { _, alarm -> alarm.copy(ringtone = ringtone) }
    }

    override suspend fun getChainedAlarm(chainedAlarmId: Long): ChainedAlarm? = null

    override suspend fun getChainedAlarmsForAlarm(alarmId: Long): List<ChainedAlarm> =
        alarms[alarmId]?.chainedAlarms.orEmpty()

    override suspend fun getAllChainedAlarms(): List<ChainedAlarm> =
        alarms.values.flatMap { it.chainedAlarms }

    override suspend fun setChainedAlarmNextTime(
        chainedAlarmId: Long,
        nextTimeInMillis: Long?
    ) {
        alarms.replaceAll { _, alarm ->
            alarm.copy(
                chainedAlarms = alarm.chainedAlarms.map { chainedAlarm ->
                    if (chainedAlarm.chainedAlarmId == chainedAlarmId) {
                        chainedAlarm.copy(nextChainedAlarmTimeInMillis = nextTimeInMillis)
                    } else chainedAlarm
                }
            )
        }
    }

    override suspend fun clearChainedAlarmsNextTimes(alarmId: Long) {
        alarms.computeIfPresent(alarmId) { _, alarm ->
            alarm.copy(
                chainedAlarms = alarm.chainedAlarms.map {
                    it.copy(nextChainedAlarmTimeInMillis = null)
                }
            )
        }
    }
}
