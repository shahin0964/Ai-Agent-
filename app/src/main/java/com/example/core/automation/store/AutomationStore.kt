package com.example.core.automation.store

import android.content.Context
import android.content.SharedPreferences
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationCategory
import com.example.core.automation.model.AutomationConfirmationPolicy
import com.example.core.automation.model.AutomationExecutionStatus
import com.example.core.automation.model.AutomationHistory
import com.example.core.automation.model.AutomationNotificationPolicy
import com.example.core.automation.model.AutomationState
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.model.ConfirmationPolicyType
import com.example.core.automation.model.ExecutionPolicy
import com.example.core.automation.model.RecurrenceType
import com.example.core.automation.model.StepExecutionRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Patch 6 Automation & History Persistent Store.
 * Provides thread-safe persistent CRUD, history logging, and JSON export/import.
 */
class AutomationStore(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_automation_store_prefs", Context.MODE_PRIVATE)

  private val historyPrefs: SharedPreferences =
    context.getSharedPreferences("aegis_automation_history_prefs", Context.MODE_PRIVATE)

  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val _automations = MutableStateFlow<List<Automation>>(emptyList())
  val automations: StateFlow<List<Automation>> = _automations.asStateFlow()

  private val _history = MutableStateFlow<List<AutomationHistory>>(emptyList())
  val history: StateFlow<List<AutomationHistory>> = _history.asStateFlow()

  init {
    loadAutomations()
    loadHistory()
  }

  @Synchronized
  fun loadAutomations(): List<Automation> {
    val rawList = prefs.getStringSet("automation_ids", emptySet()) ?: emptySet()
    val list = mutableListOf<Automation>()

    for (id in rawList) {
      val json = prefs.getString("auto_$id", null)
      if (json != null) {
        try {
          deserializeAutomation(json)?.let { list.add(it) }
        } catch (_: Exception) {}
      }
    }

    if (list.isEmpty()) {
      // Seed default draft templates
      val defaults = createDefaultDraftTemplates()
      defaults.forEach { saveInternal(it) }
      list.addAll(defaults)
    }

    _automations.value = list.sortedByDescending { it.updatedAt }
    return _automations.value
  }

  @Synchronized
  fun getAutomation(id: String): Automation? {
    return _automations.value.firstOrNull { it.id == id }
  }

  @Synchronized
  fun saveAutomation(automation: Automation) {
    val updated = automation.copy(updatedAt = System.currentTimeMillis())
    saveInternal(updated)
    refreshAutomations()
  }

  @Synchronized
  fun updateState(id: String, state: AutomationState) {
    val current = getAutomation(id) ?: return
    val updated = current.copy(
      state = state,
      enabled = (state == AutomationState.ENABLED),
      updatedAt = System.currentTimeMillis()
    )
    saveInternal(updated)
    refreshAutomations()
  }

  @Synchronized
  fun updateRunStats(id: String, success: Boolean, nextRunAt: Long?) {
    val current = getAutomation(id) ?: return
    val updated = current.copy(
      lastRunAt = System.currentTimeMillis(),
      nextRunAt = nextRunAt,
      runCount = current.runCount + 1,
      failureCount = if (success) current.failureCount else current.failureCount + 1,
      state = if (current.trigger is AutomationTrigger.TimeSchedule &&
        (current.trigger as AutomationTrigger.TimeSchedule).recurrence == RecurrenceType.ONE_TIME && success
      ) AutomationState.COMPLETED else current.state,
      enabled = if (current.trigger is AutomationTrigger.TimeSchedule &&
        (current.trigger as AutomationTrigger.TimeSchedule).recurrence == RecurrenceType.ONE_TIME && success
      ) false else current.enabled
    )
    saveInternal(updated)
    refreshAutomations()
  }

  @Synchronized
  fun deleteAutomation(id: String) {
    prefs.edit()
      .remove("auto_$id")
      .apply()

    val currentIds = prefs.getStringSet("automation_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    currentIds.remove(id)
    prefs.edit().putStringSet("automation_ids", currentIds).apply()

    refreshAutomations()
  }

  @Synchronized
  fun duplicateAutomation(id: String): Automation? {
    val source = getAutomation(id) ?: return null
    val copy = source.copy(
      id = UUID.randomUUID().toString(),
      name = "${source.name} (Copy)",
      enabled = false,
      state = AutomationState.DRAFT,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      lastRunAt = null,
      nextRunAt = null,
      runCount = 0,
      failureCount = 0
    )
    saveInternal(copy)
    refreshAutomations()
    return copy
  }

  @Synchronized
  fun recordHistory(historyItem: AutomationHistory) {
    val currentList = _history.value.toMutableList()
    currentList.add(0, historyItem)
    if (currentList.size > 100) {
      currentList.removeAt(currentList.size - 1)
    }

    try {
      val json = serializeHistoryList(currentList)
      historyPrefs.edit().putString("history_records", json).apply()
    } catch (_: Exception) {}

    _history.value = currentList
  }

  @Synchronized
  fun loadHistory(): List<AutomationHistory> {
    val json = historyPrefs.getString("history_records", null) ?: return emptyList()
    val list = deserializeHistoryList(json)
    _history.value = list
    return list
  }

  @Synchronized
  fun clearHistory(automationId: String? = null) {
    if (automationId == null) {
      historyPrefs.edit().remove("history_records").apply()
      _history.value = emptyList()
    } else {
      val filtered = _history.value.filter { it.automationId != automationId }
      val json = serializeHistoryList(filtered)
      historyPrefs.edit().putString("history_records", json).apply()
      _history.value = filtered
    }
  }

  fun exportAutomations(): String {
    val list = _automations.value
    val exportList = list.map { it.copy(enabled = false, state = AutomationState.DRAFT) }
    return serializeAutomationList(exportList)
  }

  fun importAutomations(json: String): Int {
    val list = deserializeAutomationList(json)
    var importedCount = 0
    for (item in list) {
      val newId = UUID.randomUUID().toString()
      val safeItem = item.copy(
        id = newId,
        enabled = false,
        state = AutomationState.DRAFT,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastRunAt = null,
        nextRunAt = null
      )
      saveInternal(safeItem)
      importedCount++
    }
    refreshAutomations()
    return importedCount
  }

  private fun saveInternal(automation: Automation) {
    try {
      val json = serializeAutomation(automation)
      prefs.edit().putString("auto_${automation.id}", json).apply()

      val currentIds = prefs.getStringSet("automation_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
      currentIds.add(automation.id)
      prefs.edit().putStringSet("automation_ids", currentIds).apply()
    } catch (_: Exception) {}
  }

  private fun refreshAutomations() {
    val rawList = prefs.getStringSet("automation_ids", emptySet()) ?: emptySet()
    val list = mutableListOf<Automation>()
    for (id in rawList) {
      val json = prefs.getString("auto_$id", null)
      if (json != null) {
        deserializeAutomation(json)?.let { list.add(it) }
      }
    }
    _automations.value = list.sortedByDescending { it.updatedAt }
  }

  private fun createDefaultDraftTemplates(): List<Automation> {
    return listOf(
      Automation(
        id = UUID.randomUUID().toString(),
        name = "Morning Intelligence Briefing",
        description = "Researches morning tech and global news, generates executive AI summary, and delivers notification.",
        category = AutomationCategory.NEWS,
        enabled = false,
        state = AutomationState.DRAFT,
        trigger = AutomationTrigger.TimeSchedule(
          timeHour = 8,
          timeMinute = 0,
          recurrence = RecurrenceType.DAILY
        ),
        actions = listOf(
          AgentAction(
            type = ActionType.CHECK_CONNECTIVITY,
            description = "Verify network connection",
            requiredCapabilities = emptyList()
          ),
          AgentAction(
            type = ActionType.WEB_RESEARCH,
            description = "Research today's top tech news and AI breakthroughs",
            requiredCapabilities = emptyList(),
            parameters = mapOf("query" to "top technology and AI news today", "depth" to "standard")
          ),
          AgentAction(
            type = ActionType.AI_REQUEST,
            description = "Summarize top 3 news points in bullet format",
            requiredCapabilities = emptyList(),
            parameters = mapOf("prompt" to "Provide a 3-bullet morning briefing based on: \$researchResult")
          ),
          AgentAction(
            type = ActionType.SEND_NOTIFICATION,
            description = "Deliver briefing notification",
            requiredCapabilities = emptyList(),
            parameters = mapOf("title" to "Morning Intelligence Briefing", "body" to "\$previousResult")
          )
        ),
        executionPolicy = ExecutionPolicy(networkRequired = true),
        notificationPolicy = AutomationNotificationPolicy(showSummaryNotification = true)
      ),
      Automation(
        id = UUID.randomUUID().toString(),
        name = "Evening Rest & Sleep Reminder",
        description = "Reminds the user to wind down and prepare for sleep at 10:30 PM.",
        category = AutomationCategory.COMPANION,
        enabled = false,
        state = AutomationState.DRAFT,
        trigger = AutomationTrigger.TimeSchedule(
          timeHour = 22,
          timeMinute = 30,
          recurrence = RecurrenceType.DAILY
        ),
        actions = listOf(
          AgentAction(
            type = ActionType.SEND_NOTIFICATION,
            description = "Send evening quiet reminder",
            requiredCapabilities = emptyList(),
            parameters = mapOf(
              "title" to "Evening Rest Reminder",
              "body" to "It's 10:30 PM. Time to wind down for restful sleep. Goodnight!"
            )
          )
        ),
        notificationPolicy = AutomationNotificationPolicy(notificationsEnabled = true)
      ),
      Automation(
        id = UUID.randomUUID().toString(),
        name = "Low Battery Power Guard",
        description = "Notifies when battery level drops below 20% to connect power.",
        category = AutomationCategory.DEVICE,
        enabled = false,
        state = AutomationState.DRAFT,
        trigger = AutomationTrigger.BatteryEvent(
          triggerOnLow = true,
          thresholdPercent = 20,
          triggerOnCharging = false
        ),
        actions = listOf(
          AgentAction(
            type = ActionType.SEND_NOTIFICATION,
            description = "Battery warning notification",
            requiredCapabilities = emptyList(),
            parameters = mapOf(
              "title" to "Battery Low (20%)",
              "body" to "Device battery is low. Please plug in your charger to maintain agent standby."
            )
          )
        )
      )
    )
  }

  // Robust serialization / deserialization helpers
  private fun serializeAutomation(automation: Automation): String {
    // Save to SharedPreferences as key-value representation
    val triggerJson = when (val t = automation.trigger) {
      is AutomationTrigger.TimeSchedule -> "TIME|${t.timeHour}|${t.timeMinute}|${t.recurrence.name}|${t.daysOfWeek.joinToString(",")}|${t.dayOfMonth ?: ""}|${t.intervalMinutes ?: ""}"
      is AutomationTrigger.BatteryEvent -> "BATTERY|${t.triggerOnLow}|${t.thresholdPercent}|${t.triggerOnCharging}"
      is AutomationTrigger.ConnectivityEvent -> "CONN|${t.triggerOnConnected}|${t.wifiOnly}"
      is AutomationTrigger.DeviceEvent -> "DEVICE|${t.eventType}"
      is AutomationTrigger.NotificationEvent -> "NOTIF|${t.appPackage ?: ""}|${t.keyword ?: ""}"
      is AutomationTrigger.LocationEvent -> "LOC|${t.latitude}|${t.longitude}|${t.radiusMeters}|${t.isEntering}"
      is AutomationTrigger.CalendarEvent -> "CAL|${t.keyword ?: ""}|${t.minutesBefore}"
      is AutomationTrigger.VoiceCommandTrigger -> "VOX|${t.phrasePattern}"
      is AutomationTrigger.ManualTrigger -> "MANUAL"
      is AutomationTrigger.AppEvent -> "APP|${t.eventName}"
      is AutomationTrigger.SystemEvent -> "SYS|${t.eventName}"
    }

    val actionsData = automation.actions.map { a ->
      "${a.id}:::${a.type.name}:::${a.description.replace("\n", " ")}:::${a.requiresConfirmation}:::${serializeMap(a.parameters)}"
    }.joinToString(";;;")

    return "${automation.id}###${automation.name}###${automation.description}###${automation.category.name}###${automation.enabled}###${automation.state.name}###$triggerJson###$actionsData###${automation.createdAt}###${automation.updatedAt}###${automation.lastRunAt ?: ""}###${automation.nextRunAt ?: ""}###${automation.runCount}###${automation.failureCount}###${automation.executionPolicy.networkRequired}###${automation.executionPolicy.chargingRequired}###${automation.executionPolicy.minBatteryLevel}###${automation.notificationPolicy.notificationsEnabled}###${automation.confirmationPolicy.policyType.name}###${automation.timezone}"
  }

  private fun deserializeAutomation(raw: String): Automation? {
    return try {
      val parts = raw.split("###")
      if (parts.size < 8) return null

      val id = parts[0]
      val name = parts[1]
      val description = parts[2]
      val category = try { AutomationCategory.valueOf(parts[3]) } catch (_: Exception) { AutomationCategory.PERSONAL }
      val enabled = parts[4].toBoolean()
      val state = try { AutomationState.valueOf(parts[5]) } catch (_: Exception) { AutomationState.DRAFT }
      val trigger = parseTrigger(parts[6])
      val actions = parseActions(parts[7])
      val createdAt = parts.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis()
      val updatedAt = parts.getOrNull(9)?.toLongOrNull() ?: System.currentTimeMillis()
      val lastRunAt = parts.getOrNull(10)?.toLongOrNull()
      val nextRunAt = parts.getOrNull(11)?.toLongOrNull()
      val runCount = parts.getOrNull(12)?.toIntOrNull() ?: 0
      val failureCount = parts.getOrNull(13)?.toIntOrNull() ?: 0
      val netReq = parts.getOrNull(14)?.toBoolean() ?: false
      val chargeReq = parts.getOrNull(15)?.toBoolean() ?: false
      val minBatt = parts.getOrNull(16)?.toIntOrNull() ?: 0
      val notifEn = parts.getOrNull(17)?.toBoolean() ?: true
      val confPolicy = try {
        ConfirmationPolicyType.valueOf(parts.getOrNull(18) ?: "")
      } catch (_: Exception) { ConfirmationPolicyType.NO_CONFIRMATION_FOR_SAFE_ACTIONS }
      val tz = parts.getOrNull(19) ?: java.util.TimeZone.getDefault().id

      Automation(
        id = id,
        name = name,
        description = description,
        category = category,
        enabled = enabled,
        state = state,
        trigger = trigger,
        actions = actions,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastRunAt = lastRunAt,
        nextRunAt = nextRunAt,
        runCount = runCount,
        failureCount = failureCount,
        executionPolicy = ExecutionPolicy(networkRequired = netReq, chargingRequired = chargeReq, minBatteryLevel = minBatt),
        notificationPolicy = AutomationNotificationPolicy(notificationsEnabled = notifEn),
        confirmationPolicy = AutomationConfirmationPolicy(policyType = confPolicy),
        timezone = tz
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun parseTrigger(raw: String): AutomationTrigger {
    val segs = raw.split("|")
    return when (segs[0]) {
      "TIME" -> {
        val h = segs.getOrNull(1)?.toIntOrNull() ?: 8
        val m = segs.getOrNull(2)?.toIntOrNull() ?: 0
        val rec = try { RecurrenceType.valueOf(segs.getOrNull(3) ?: "") } catch (_: Exception) { RecurrenceType.DAILY }
        val days = segs.getOrNull(4)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        val dom = segs.getOrNull(5)?.toIntOrNull()
        val intMin = segs.getOrNull(6)?.toIntOrNull()
        AutomationTrigger.TimeSchedule(h, m, rec, days, dom, null, intMin)
      }
      "BATTERY" -> {
        val onLow = segs.getOrNull(1)?.toBoolean() ?: true
        val thresh = segs.getOrNull(2)?.toIntOrNull() ?: 20
        val onCharge = segs.getOrNull(3)?.toBoolean() ?: false
        AutomationTrigger.BatteryEvent(onLow, thresh, onCharge)
      }
      "CONN" -> {
        val onConn = segs.getOrNull(1)?.toBoolean() ?: true
        val wifi = segs.getOrNull(2)?.toBoolean() ?: false
        AutomationTrigger.ConnectivityEvent(onConn, wifi)
      }
      "DEVICE" -> AutomationTrigger.DeviceEvent(segs.getOrNull(1) ?: "GENERIC")
      "NOTIF" -> AutomationTrigger.NotificationEvent(segs.getOrNull(1), segs.getOrNull(2))
      "LOC" -> {
        val lat = segs.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val lon = segs.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        val rad = segs.getOrNull(3)?.toFloatOrNull() ?: 100f
        val entering = segs.getOrNull(4)?.toBoolean() ?: true
        AutomationTrigger.LocationEvent(lat, lon, rad, entering)
      }
      "CAL" -> AutomationTrigger.CalendarEvent(segs.getOrNull(1), segs.getOrNull(2)?.toIntOrNull() ?: 15)
      "VOX" -> AutomationTrigger.VoiceCommandTrigger(segs.getOrNull(1) ?: "")
      "APP" -> AutomationTrigger.AppEvent(segs.getOrNull(1) ?: "")
      "SYS" -> AutomationTrigger.SystemEvent(segs.getOrNull(1) ?: "")
      else -> AutomationTrigger.ManualTrigger
    }
  }

  private fun parseActions(raw: String): List<AgentAction> {
    if (raw.isBlank()) return emptyList()
    val list = mutableListOf<AgentAction>()
    val chunks = raw.split(";;;")
    for (chunk in chunks) {
      if (chunk.isBlank()) continue
      val fields = chunk.split(":::")
      if (fields.size >= 3) {
        val id = fields[0]
        val type = try { ActionType.valueOf(fields[1]) } catch (_: Exception) { ActionType.SEND_NOTIFICATION }
        val desc = fields[2]
        val reqConf = fields.getOrNull(3)?.toBoolean() ?: false
        val params = deserializeMap(fields.getOrNull(4) ?: "")
        list.add(
          AgentAction(
            id = id,
            type = type,
            description = desc,
            requiredCapabilities = emptyList(),
            requiresConfirmation = reqConf,
            parameters = params
          )
        )
      }
    }
    return list
  }

  private fun serializeMap(map: Map<String, Any?>): String {
    return map.entries.joinToString("&") { "${it.key}=${it.value?.toString()?.replace("&", " ")?.replace("=", " ")}" }
  }

  private fun deserializeMap(raw: String): Map<String, Any?> {
    if (raw.isBlank()) return emptyMap()
    val map = mutableMapOf<String, Any?>()
    val pairs = raw.split("&")
    for (p in pairs) {
      val kv = p.split("=")
      if (kv.size == 2) {
        map[kv[0]] = kv[1]
      }
    }
    return map
  }

  private fun serializeHistoryList(list: List<AutomationHistory>): String {
    return list.joinToString("|||") { h ->
      val steps = h.stepsLog.joinToString("^^^") { s ->
        "${s.stepIndex}~${s.actionTitle}~${s.actionType}~${s.timestamp}~${s.status}~${s.details ?: ""}"
      }
      "${h.executionId}%%%${h.automationId}%%%${h.automationName}%%%${h.triggerType}%%%${h.status.name}%%%${h.startedAt}%%%${h.durationMs}%%%${h.completedSteps}%%%${h.totalSteps}%%%${h.failedStep ?: ""}%%%${h.errorMessage ?: ""}%%%${h.providerUsed ?: ""}%%%$steps"
    }
  }

  private fun deserializeHistoryList(raw: String): List<AutomationHistory> {
    if (raw.isBlank()) return emptyList()
    val list = mutableListOf<AutomationHistory>()
    val chunks = raw.split("|||")
    for (c in chunks) {
      if (c.isBlank()) continue
      val fields = c.split("%%%")
      if (fields.size >= 9) {
        val execId = fields[0]
        val autoId = fields[1]
        val name = fields[2]
        val trig = fields[3]
        val status = try { AutomationExecutionStatus.valueOf(fields[4]) } catch (_: Exception) { AutomationExecutionStatus.SUCCESS }
        val start = fields[5].toLongOrNull() ?: 0L
        val dur = fields[6].toLongOrNull() ?: 0L
        val comp = fields[7].toIntOrNull() ?: 0
        val tot = fields[8].toIntOrNull() ?: 0
        val failStep = fields.getOrNull(9)?.takeIf { it.isNotBlank() }
        val err = fields.getOrNull(10)?.takeIf { it.isNotBlank() }
        val prov = fields.getOrNull(11)?.takeIf { it.isNotBlank() }
        val stepsRaw = fields.getOrNull(12) ?: ""
        val steps = if (stepsRaw.isNotBlank()) {
          stepsRaw.split("^^^").mapNotNull { st ->
            val sf = st.split("~")
            if (sf.size >= 5) {
              StepExecutionRecord(
                stepIndex = sf[0].toIntOrNull() ?: 0,
                actionTitle = sf[1],
                actionType = sf[2],
                timestamp = sf[3].toLongOrNull() ?: 0L,
                status = sf[4],
                details = sf.getOrNull(5)?.takeIf { it.isNotBlank() }
              )
            } else null
          }
        } else emptyList()

        list.add(
          AutomationHistory(
            executionId = execId,
            automationId = autoId,
            automationName = name,
            triggerType = trig,
            status = status,
            startedAt = start,
            durationMs = dur,
            completedSteps = comp,
            totalSteps = tot,
            failedStep = failStep,
            errorMessage = err,
            providerUsed = prov,
            stepsLog = steps
          )
        )
      }
    }
    return list
  }

  private fun serializeAutomationList(list: List<Automation>): String {
    return list.joinToString("@@@") { serializeAutomation(it) }
  }

  private fun deserializeAutomationList(raw: String): List<Automation> {
    if (raw.isBlank()) return emptyList()
    return raw.split("@@@").mapNotNull { deserializeAutomation(it) }
  }
}
