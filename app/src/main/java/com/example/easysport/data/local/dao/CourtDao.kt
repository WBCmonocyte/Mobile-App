package com.example.easysport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.easysport.data.local.entity.CourtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtDao {
    @Query("SELECT * FROM courts")
    fun getAllCourts(): Flow<List<CourtEntity>>

    @Query("SELECT * FROM courts")
    suspend fun getAllCourtsSync(): List<CourtEntity>

    @Query("SELECT * FROM courts WHERE id = :id LIMIT 1")
    suspend fun getCourtById(id: String): CourtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourts(courts: List<CourtEntity>)
}
