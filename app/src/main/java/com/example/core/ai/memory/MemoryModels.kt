package com.example.core.ai.memory

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MemoryCategory(val label: String) {
  USER_PREFERENCE("Preference"),
  FACTUAL("Fact / Profile"),
  PROJECT("Project / Work"),
  GENERAL("General Knowledge")
}

@Entity(tableName = "agent_memories")
data class MemoryEntity(
  @PrimaryKey
  val id: String = UUID.randomUUID().toString(),
  val content: String,
  val category: String = MemoryCategory.GENERAL.name,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val source: String = "User Command",
  val userApproved: Boolean = true,
  val enabled: Boolean = true
)

data class MemorySettings(
  val memoryEnabled: Boolean = true,
  val longTermMemoryEnabled: Boolean = true,
  val autoSaveEnabled: Boolean = false,
  val researchContextEnabled: Boolean = true,
  val voiceContextEnabled: Boolean = true
)
