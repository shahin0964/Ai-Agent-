package com.example.core.ai.memory

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
  @Query("SELECT * FROM agent_memories WHERE enabled = 1 ORDER BY createdAt DESC")
  fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

  @Query("SELECT * FROM agent_memories WHERE enabled = 1 ORDER BY createdAt DESC")
  suspend fun getAllMemories(): List<MemoryEntity>

  @Query("SELECT * FROM agent_memories WHERE content LIKE '%' || :query || '%' AND enabled = 1")
  suspend fun searchMemories(query: String): List<MemoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemory(memory: MemoryEntity)

  @Update
  suspend fun updateMemory(memory: MemoryEntity)

  @Delete
  suspend fun deleteMemory(memory: MemoryEntity)

  @Query("DELETE FROM agent_memories WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM agent_memories")
  suspend fun deleteAll()

  @Query("DELETE FROM agent_memories WHERE content LIKE '%' || :keyword || '%'")
  suspend fun deleteMatching(keyword: String): Int
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
  abstract fun memoryDao(): MemoryDao

  companion object {
    @Volatile
    private var INSTANCE: MemoryDatabase? = null

    fun getInstance(context: Context): MemoryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          MemoryDatabase::class.java,
          "aegis_agent_memory.db"
        )
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
