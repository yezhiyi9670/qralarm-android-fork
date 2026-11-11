package com.sweak.qralarm.core.data.alarm

import androidx.room.withTransaction
import com.sweak.qralarm.core.domain.alarm.Alarm
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.ChainedAlarm
import com.sweak.qralarm.core.domain.alarm.Code
import com.sweak.qralarm.core.storage.database.QRAlarmDatabase
import com.sweak.qralarm.core.storage.database.dao.AlarmsDao
import com.sweak.qralarm.core.storage.database.dao.ChainedAlarmsDao
import com.sweak.qralarm.core.storage.database.dao.CodesDao
import com.sweak.qralarm.core.storage.database.model.AlarmEntity
import com.sweak.qralarm.core.storage.database.model.ChainedAlarmEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.time.DayOfWeek
import javax.inject.Inject

class AlarmsRepositoryImpl @Inject constructor(
    private val database: QRAlarmDatabase,
    private val alarmsDao: AlarmsDao,
    private val chainedAlarmsDao: ChainedAlarmsDao,
    private val codesDao: CodesDao
) : AlarmsRepository {

    override suspend fun addOrEditAlarm(
        alarm: Alarm,
        chainedAlarms: List<ChainedAlarm>
    ): Long = database.withTransaction {
        val upsertedAlarmId = alarmsDao.upsertAlarm(
            alarmEntity = AlarmEntity(
                alarmId = alarm.alarmId,
                alarmHourOfDay = alarm.alarmHourOfDay,
                alarmMinute = alarm.alarmMinute,
                isAlarmEnabled = alarm.isAlarmEnabled,
                isAlarmRunning = alarm.isAlarmRunning,
                nextAlarmTimeInMillis = alarm.nextAlarmTimeInMillis,
                repeatingAlarmDays =
                    if (alarm.repeatingMode is Alarm.RepeatingMode.Days) {
                        alarm.repeatingMode.repeatingDaysOfWeek.joinToString { it.name }
                    } else null,
                numberOfSnoozes = alarm.snoozeConfig.snoozeMode.numberOfSnoozes,
                snoozeDurationInMinutes = alarm.snoozeConfig.snoozeMode.snoozeDurationInMinutes,
                numberOfSnoozesLeft = alarm.snoozeConfig.numberOfSnoozesLeft,
                isAlarmSnoozed = alarm.snoozeConfig.isAlarmSnoozed,
                nextSnoozedAlarmTimeInMillis = alarm.snoozeConfig.nextSnoozedAlarmTimeInMillis,
                ringtone = alarm.ringtone.name,
                customRingtoneUriString = alarm.customRingtoneUriString,
                customRingtoneName = alarm.customRingtoneName,
                alarmVolumePercentage =
                    if (alarm.alarmVolumeMode is Alarm.AlarmVolumeMode.Custom) {
                        alarm.alarmVolumeMode.volumePercentage
                    } else 0,
                areVibrationsEnabled = alarm.areVibrationsEnabled,
                isUsingCode = alarm.isUsingCode,
                assignedCodeId = alarm.assignedCode?.codeId,
                isOpenCodeLinkEnabled = alarm.isOpenCodeLinkEnabled,
                cancelLockDurationInMinutes = alarm.cancelLockDurationInMinutes,
                isEmergencyTaskEnabled = alarm.isEmergencyTaskEnabled,
                alarmLabel = alarm.alarmLabel,
                gentleWakeUpDurationInSeconds = alarm.gentleWakeUpDurationInSeconds,
                temporaryMuteDurationInSeconds = alarm.temporaryMuteDurationInSeconds,
                skipAlarmUntilTimeInMillis = alarm.skipAlarmUntilTimeInMillis
            )
        )

        val alarmId = upsertedAlarmId.takeIf { it > 0 } ?: alarm.alarmId

        val existingChainedAlarms = chainedAlarmsDao.getChainedAlarms(parentAlarmId = alarmId)
        val keptChainedAlarmIds = chainedAlarms
            .mapNotNull { it.chainedAlarmId.takeIf { id -> id > 0 } }
            .toSet()

        existingChainedAlarms
            .filterNot { it.chainedAlarmId in keptChainedAlarmIds }
            .forEach { chainedAlarm ->
                chainedAlarmsDao.deleteChainedAlarm(chainedAlarm.chainedAlarmId)
            }

        chainedAlarms.forEachIndexed { index, chainedAlarm ->
            chainedAlarmsDao.upsertChainedAlarm(
                ChainedAlarmEntity(
                    chainedAlarmId =
                        chainedAlarm.chainedAlarmId.takeIf { it > 0 } ?: 0,
                    parentAlarmId = alarmId,
                    delayMinutes = chainedAlarm.delayMinutes,
                    assignedCodeId = chainedAlarm.assignedCode?.codeId,
                    label = chainedAlarm.label,
                    order = index,
                    nextChainedAlarmTimeInMillis =
                        chainedAlarm.nextChainedAlarmTimeInMillis
                )
            )
        }

        alarmId
    }

    override suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(isAlarmEnabled = enabled)
            )
        }
    }

    override suspend fun setAlarmRunning(alarmId: Long, running: Boolean) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(isAlarmRunning = running)
            )
        }
    }

    override suspend fun setAlarmSnoozed(alarmId: Long, snoozed: Boolean) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(
                    isAlarmSnoozed = snoozed,
                    nextSnoozedAlarmTimeInMillis =
                        if (!snoozed) null else alarmEntity.nextSnoozedAlarmTimeInMillis
                )
            )
        }
    }

    override suspend fun setSkipNextAlarm(alarmId: Long, skip: Boolean) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(
                    skipAlarmUntilTimeInMillis =
                        if (skip) alarmEntity.nextAlarmTimeInMillis else null
                )
            )
        }
    }

    override suspend fun setAvailableSnoozes(alarmId: Long, availableSnoozes: Int) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(numberOfSnoozesLeft = availableSnoozes)
            )
        }
    }

    override suspend fun setAlarmRingtoneUri(alarmId: Long, uri: String?) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(customRingtoneUriString = uri)
            )
        }
    }

    override suspend fun setAlarmCustomRingtoneName(alarmId: Long, name: String?) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(customRingtoneName = name)
            )
        }
    }

    override suspend fun setAlarmRingtone(alarmId: Long, ringtone: Alarm.Ringtone) {
        alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            alarmsDao.upsertAlarm(
                alarmEntity = alarmEntity.copy(ringtone = ringtone.name)
            )
        }
    }

    override suspend fun getAlarm(alarmId: Long): Alarm? {
        return alarmsDao.getAlarm(alarmId = alarmId).firstOrNull()?.let { alarmEntity ->
            convertAlarmEntity(alarmEntity = alarmEntity)
        }
    }

    override fun getAlarmFlow(alarmId: Long): Flow<Alarm> {
        return alarmsDao.getAlarm(alarmId = alarmId).mapNotNull { alarmEntity ->
            alarmEntity?.let { convertAlarmEntity(alarmEntity = it) }
        }
    }

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmsDao.getAllAlarms().map { alarmEntityList ->
            alarmEntityList.map { alarmEntity ->
                convertAlarmEntity(alarmEntity = alarmEntity)
            }
        }
    }

    override suspend fun deleteAlarm(alarmId: Long) {
        alarmsDao.deleteAlarm(alarmId = alarmId)
    }

    override suspend fun getChainedAlarm(chainedAlarmId: Long): ChainedAlarm? =
        chainedAlarmsDao.getChainedAlarm(chainedAlarmId)?.let { entity ->
            convertChainedAlarmEntity(entity)
        }

    override suspend fun getChainedAlarmsForAlarm(alarmId: Long): List<ChainedAlarm> {
        val chainedAlarms = mutableListOf<ChainedAlarm>()

        for (entity in chainedAlarmsDao.getChainedAlarms(parentAlarmId = alarmId)) {
            chainedAlarms.add(convertChainedAlarmEntity(entity))
        }

        return chainedAlarms
    }

    override suspend fun getAllChainedAlarms(): List<ChainedAlarm> {
        val chainedAlarms = mutableListOf<ChainedAlarm>()

        for (entity in chainedAlarmsDao.getAllChainedAlarms()) {
            chainedAlarms.add(convertChainedAlarmEntity(entity))
        }

        return chainedAlarms
    }

    override suspend fun setChainedAlarmNextTime(chainedAlarmId: Long, nextTimeInMillis: Long?) {
        chainedAlarmsDao.setChainedAlarmNextTime(
            chainedAlarmId = chainedAlarmId,
            nextTimeInMillis = nextTimeInMillis
        )
    }

    override suspend fun clearChainedAlarmsNextTimes(alarmId: Long) {
        chainedAlarmsDao.getChainedAlarms(parentAlarmId = alarmId).forEach { chainedAlarm ->
            if (chainedAlarm.nextChainedAlarmTimeInMillis != null) {
                chainedAlarmsDao.setChainedAlarmNextTime(
                    chainedAlarmId = chainedAlarm.chainedAlarmId,
                    nextTimeInMillis = null
                )
            }
        }
    }

    private suspend fun convertAlarmEntity(alarmEntity: AlarmEntity): Alarm {
        val repeatingMode = if (alarmEntity.repeatingAlarmDays != null) {
            Alarm.RepeatingMode.Days(
                repeatingDaysOfWeek = alarmEntity.repeatingAlarmDays.split(", ").map {
                    DayOfWeek.valueOf(it)
                }
            )
        } else {
            Alarm.RepeatingMode.Once
        }

        return Alarm(
            alarmId = alarmEntity.alarmId,
            alarmHourOfDay = alarmEntity.alarmHourOfDay,
            alarmMinute = alarmEntity.alarmMinute,
            isAlarmEnabled = alarmEntity.isAlarmEnabled,
            isAlarmRunning = alarmEntity.isAlarmRunning,
            repeatingMode = repeatingMode,
            nextAlarmTimeInMillis = alarmEntity.nextAlarmTimeInMillis,
            snoozeConfig = Alarm.SnoozeConfig(
                snoozeMode = Alarm.SnoozeMode(
                    numberOfSnoozes = alarmEntity.numberOfSnoozes,
                    snoozeDurationInMinutes = alarmEntity.snoozeDurationInMinutes
                ),
                numberOfSnoozesLeft = alarmEntity.numberOfSnoozesLeft,
                isAlarmSnoozed = alarmEntity.isAlarmSnoozed,
                nextSnoozedAlarmTimeInMillis = alarmEntity.nextSnoozedAlarmTimeInMillis
            ),
            ringtone = Alarm.Ringtone.valueOf(alarmEntity.ringtone),
            customRingtoneUriString = alarmEntity.customRingtoneUriString,
            customRingtoneName = alarmEntity.customRingtoneName,
            alarmVolumeMode = alarmEntity.alarmVolumePercentage.run {
                if (this < 10) Alarm.AlarmVolumeMode.System
                else Alarm.AlarmVolumeMode.Custom(volumePercentage = this)
            },
            areVibrationsEnabled = alarmEntity.areVibrationsEnabled,
            isUsingCode = alarmEntity.isUsingCode,
            assignedCode = resolveCode(alarmEntity.assignedCodeId),
            isOpenCodeLinkEnabled = alarmEntity.isOpenCodeLinkEnabled,
            cancelLockDurationInMinutes = alarmEntity.cancelLockDurationInMinutes,
            isEmergencyTaskEnabled = alarmEntity.isEmergencyTaskEnabled,
            alarmLabel = alarmEntity.alarmLabel,
            gentleWakeUpDurationInSeconds = alarmEntity.gentleWakeUpDurationInSeconds,
            temporaryMuteDurationInSeconds = alarmEntity.temporaryMuteDurationInSeconds,
            skipAlarmUntilTimeInMillis = alarmEntity.skipAlarmUntilTimeInMillis,
            chainedAlarms = getChainedAlarmsForAlarm(alarmId = alarmEntity.alarmId)
        )
    }

    private suspend fun convertChainedAlarmEntity(
        chainedAlarmEntity: ChainedAlarmEntity
    ): ChainedAlarm = ChainedAlarm(
        chainedAlarmId = chainedAlarmEntity.chainedAlarmId,
        delayMinutes = chainedAlarmEntity.delayMinutes,
        assignedCode = resolveCode(chainedAlarmEntity.assignedCodeId),
        label = chainedAlarmEntity.label,
        order = chainedAlarmEntity.order,
        nextChainedAlarmTimeInMillis = chainedAlarmEntity.nextChainedAlarmTimeInMillis
    )

    private suspend fun resolveCode(codeId: Long?): Code? =
        codeId?.let { id ->
            codesDao.getCode(id)?.let { Code(codeId = it.codeId, value = it.value, name = it.name) }
        }
}
