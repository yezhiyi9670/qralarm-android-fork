package com.sweak.qralarm.core.domain.alarm

import kotlinx.coroutines.flow.Flow

interface AlarmsRepository {
    suspend fun addOrEditAlarm(
        alarm: Alarm,
        chainedAlarms: List<ChainedAlarm> = emptyList()
    ): Long

    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean)
    suspend fun setAlarmRunning(alarmId: Long, running: Boolean)
    suspend fun setAlarmSnoozed(alarmId: Long, snoozed: Boolean)
    suspend fun setSkipNextAlarm(alarmId: Long, skip: Boolean)
    suspend fun getAlarm(alarmId: Long): Alarm?
    fun getAlarmFlow(alarmId: Long): Flow<Alarm>
    fun getAllAlarms(): Flow<List<Alarm>>
    suspend fun deleteAlarm(alarmId: Long)
    suspend fun setAvailableSnoozes(alarmId: Long, availableSnoozes: Int)
    suspend fun setAlarmRingtoneUri(alarmId: Long, uri: String?)
    suspend fun setAlarmCustomRingtoneName(alarmId: Long, name: String?)
    suspend fun setAlarmRingtone(alarmId: Long, ringtone: Alarm.Ringtone)

    suspend fun getChainedAlarm(chainedAlarmId: Long): ChainedAlarm?
    suspend fun getChainedAlarmsForAlarm(alarmId: Long): List<ChainedAlarm>
    suspend fun getAllChainedAlarms(): List<ChainedAlarm>
    suspend fun setChainedAlarmNextTime(chainedAlarmId: Long, nextTimeInMillis: Long?)
    suspend fun clearChainedAlarmsNextTimes(alarmId: Long)
}
