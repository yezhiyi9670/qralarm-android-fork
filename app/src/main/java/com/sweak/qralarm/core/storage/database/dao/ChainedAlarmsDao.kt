package com.sweak.qralarm.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.sweak.qralarm.core.storage.database.model.ChainedAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChainedAlarmsDao {

    @Query("SELECT * FROM chainedAlarm WHERE parentAlarmId = :parentAlarmId ORDER BY `order`")
    suspend fun getChainedAlarms(parentAlarmId: Long): List<ChainedAlarmEntity>

    @Query("SELECT * FROM chainedAlarm WHERE parentAlarmId = :parentAlarmId ORDER BY `order`")
    fun getChainedAlarmsFlow(parentAlarmId: Long): Flow<List<ChainedAlarmEntity>>

    @Query("SELECT * FROM chainedAlarm ORDER BY parentAlarmId, `order`")
    suspend fun getAllChainedAlarms(): List<ChainedAlarmEntity>

    @Query("SELECT * FROM chainedAlarm WHERE chainedAlarmId = :chainedAlarmId")
    suspend fun getChainedAlarm(chainedAlarmId: Long): ChainedAlarmEntity?

    @Upsert
    suspend fun upsertChainedAlarm(chainedAlarmEntity: ChainedAlarmEntity)

    @Insert
    suspend fun insertChainedAlarms(chainedAlarmEntities: List<ChainedAlarmEntity>)

    @Query("UPDATE chainedAlarm SET nextChainedAlarmTimeInMillis = :nextTimeInMillis WHERE chainedAlarmId = :chainedAlarmId")
    suspend fun setChainedAlarmNextTime(chainedAlarmId: Long, nextTimeInMillis: Long?)

    @Query("DELETE FROM chainedAlarm WHERE chainedAlarmId = :chainedAlarmId")
    suspend fun deleteChainedAlarm(chainedAlarmId: Long)

    @Query("DELETE FROM chainedAlarm WHERE parentAlarmId = :parentAlarmId")
    suspend fun deleteChainedAlarmsForAlarm(parentAlarmId: Long)

    @Query("DELETE FROM chainedAlarm")
    suspend fun deleteAllChainedAlarms()
}
