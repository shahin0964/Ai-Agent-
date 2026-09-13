package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.ai.creator.CreatorOutput
import com.example.core.ai.creator.CreatorOutputType
import com.example.core.ai.memory.MemoryEntity
import com.example.core.ai.personality.PersonalityArchetype
import com.example.core.ai.research.NewsCategory
import com.example.core.ai.research.ResearchResult
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

/**
 * Memory Substrate Screen (Room DB + Short/Long Term Persistence).
 */
@Composable
fun MemoryScreen(
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val memoryManager = agentController?.aiEngine?.memoryManager

  val settings by memoryManager?.settings?.collectAsState()
    ?: remember { mutableStateOf(com.example.core.ai.memory.MemorySettings()) }

  val memories by (memoryManager?.allMemoriesFlow?.collectAsState(initial = emptyList()))
    ?: remember { mutableStateOf(emptyList()) }

  var newMemoryText by remember { mutableStateOf("") }
  var searchQuery by remember { mutableStateOf("") }

  val filteredMemories = remember(memories, searchQuery) {
    if (searchQuery.isBlank()) memories
    else memories.filter { it.content.contains(searchQuery, ignoreCase = true) }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "MEMORY SUBSTRATE // RECALL",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
        )
        StatusBadge(
          text = if (settings.memoryEnabled) "ACTIVE (${memories.size} ENTRIES)" else "MEMORY DISABLED",
          indicatorColor = if (settings.memoryEnabled) StatusReady else StatusOffline,
          isPulsing = settings.memoryEnabled
        )
      }
    }

    item {
      FuturisticCard(headerText = "Memory System Controls", badgeText = "Room Local DB") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Memory Subsystem", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
            Text("Enables agent to retain instructions and contextual facts.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
          }
          Switch(
            checked = settings.memoryEnabled,
            onCheckedChange = { memoryManager?.updateSettings(settings.copy(memoryEnabled = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = Color(0x3300F0FF))
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Long-Term Persistence", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
            Text("Persists facts to local encrypted SQLite storage across restarts.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
          }
          Switch(
            checked = settings.longTermMemoryEnabled,
            onCheckedChange = { memoryManager?.updateSettings(settings.copy(longTermMemoryEnabled = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = Color(0x3300F0FF))
          )
        }
      }
    }

    // Add Memory Input
    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("RECORD NEW MEMORY", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = newMemoryText,
            onValueChange = { newMemoryText = it },
            placeholder = { Text("e.g. User prefers dark mode and concise code snippets", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
            modifier = Modifier.fillMaxWidth().testTag("add_memory_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = Color(0x3300F0FF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = CyberTokens.shapeSmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          FuturisticButton(
            text = "Store in Memory",
            onClick = {
              if (newMemoryText.isNotBlank()) {
                scope.launch {
                  memoryManager?.saveMemoryExplicit(newMemoryText)
                  newMemoryText = ""
                }
              }
            },
            isPrimary = true,
            modifier = Modifier.fillMaxWidth().testTag("store_memory_button")
          )
        }
      }
    }

    // Search and Clear
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filter memories...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = Color(0x3300F0FF),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          shape = CyberTokens.shapeSmall,
          singleLine = true
        )

        FuturisticButton(
          text = "Clear All",
          onClick = {
            scope.launch { memoryManager?.deleteAll() }
          },
          isPrimary = false,
          modifier = Modifier.testTag("clear_memories_button")
        )
      }
    }

    if (filteredMemories.isEmpty()) {
      item {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
          Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No memories recorded yet.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
          }
        }
      }
    } else {
      items(filteredMemories, key = { it.id }) { mem ->
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = mem.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Category: ${mem.category} • Source: ${mem.source}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
              )
            }
            IconButton(onClick = { scope.launch { memoryManager?.deleteMemory(mem.id) } }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = Color.Red.copy(alpha = 0.7f))
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge)) }
  }
}

/**
 * Web Research & Live Grounding Screen.
 */
@Composable
fun ResearchScreen(
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val researchEngine = agentController?.aiEngine?.researchEngine

  var queryText by remember { mutableStateOf("") }
  var isSearching by remember { mutableStateOf(false) }
  var researchResult by remember { mutableStateOf<ResearchResult?>(null) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "COGNITIVE RECON // RESEARCH",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
        )
        StatusBadge(
          text = "LIVE GROUNDING ACTIVE",
          indicatorColor = StatusReady,
          isPulsing = false
        )
      }
    }

    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("WEB QUERY RESEARCH", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text("Enter topic or question for web research...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
            modifier = Modifier.fillMaxWidth().testTag("research_query_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = Color(0x3300F0FF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = CyberTokens.shapeSmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          FuturisticButton(
            text = if (isSearching) "Searching & Synthesizing..." else "Conduct Deep Research",
            onClick = {
              if (queryText.isNotBlank() && researchEngine != null) {
                isSearching = true
                scope.launch {
                  researchResult = researchEngine.conductResearch(queryText)
                  isSearching = false
                }
              }
            },
            isPrimary = true,
            modifier = Modifier.fillMaxWidth().testTag("conduct_research_button")
          )
        }
      }
    }

    // Category Fast Chips
    item {
      Text("LIVE NEWS CATEGORIES", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, letterSpacing = 1.sp))
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        NewsCategory.values().forEach { cat ->
          FuturisticButton(
            text = cat.label,
            onClick = {
              queryText = cat.searchQuery
              if (researchEngine != null) {
                isSearching = true
                scope.launch {
                  researchResult = researchEngine.fetchNews(cat)
                  isSearching = false
                }
              }
            },
            isPrimary = false,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    if (isSearching) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text("Querying search engines & verifying sources...", style = MaterialTheme.typography.bodySmall.copy(color = CyanNeon))
        }
      }
    }

    if (researchResult != null) {
      val res = researchResult!!
      item {
        FuturisticCard(
          headerText = "SYNTHESIZED RESEARCH INTELLIGENCE",
          badgeText = res.confidence.label
        ) {
          Text(
            text = res.synthesizedAnswer,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 20.sp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Model: ${res.modelUsed ?: "Neural Engine"} • Latency: ${res.searchLatencyMs}ms",
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
          )
        }
      }

      if (res.sources.isNotEmpty()) {
        item {
          Text("GROUNDED CITATIONS (${res.sources.size})", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
        }
        items(res.sources) { src ->
          GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = src.title,
                  style = MaterialTheme.typography.bodySmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold),
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = src.sourceDomain,
                  style = MaterialTheme.typography.labelSmall.copy(color = HologramViolet, fontSize = 10.sp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = src.snippet,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = src.url,
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 9.sp)
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge)) }
  }
}

/**
 * Developer Diagnostics & Code Generation Screen.
 */
@Composable
fun DeveloperScreen(
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val creatorEngine = agentController?.aiEngine?.creatorEngine

  var codePrompt by remember { mutableStateOf("") }
  var selectedLang by remember { mutableStateOf("Kotlin") }
  var isGenerating by remember { mutableStateOf(false) }
  var generatedOutput by remember { mutableStateOf<CreatorOutput?>(null) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "DEVELOPER CONSOLE // SYNTAX ENGINE",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
        )
        StatusBadge(text = "DEVELOPER MODE ACTIVE", indicatorColor = ElectricBlue, isPulsing = false)
      }
    }

    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("CODE GENERATOR & REFACTOR", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = codePrompt,
            onValueChange = { codePrompt = it },
            placeholder = { Text("Specify function, class, algorithm, or debugging task...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
            modifier = Modifier.fillMaxWidth().testTag("code_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = Color(0x3300F0FF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = CyberTokens.shapeSmall
          )
          Spacer(modifier = Modifier.height(10.dp))

          // Language Selector
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Kotlin", "Python", "TypeScript", "HTML/JS").forEach { lang ->
              FuturisticButton(
                text = lang,
                onClick = { selectedLang = lang },
                isPrimary = selectedLang == lang,
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          FuturisticButton(
            text = if (isGenerating) "Synthesizing Code..." else "Generate $selectedLang Code",
            onClick = {
              if (codePrompt.isNotBlank() && creatorEngine != null) {
                isGenerating = true
                scope.launch {
                  generatedOutput = creatorEngine.generateCode(codePrompt, selectedLang)
                  isGenerating = false
                }
              }
            },
            isPrimary = true,
            modifier = Modifier.fillMaxWidth().testTag("generate_code_button")
          )
        }
      }
    }

    if (isGenerating) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text("Compiling neural AST structure...", style = MaterialTheme.typography.bodySmall.copy(color = CyanNeon))
        }
      }
    }

    if (generatedOutput != null) {
      val out = generatedOutput!!
      item {
        FuturisticCard(headerText = out.title, badgeText = out.modelUsed ?: "AI Code") {
          out.files.forEach { file ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
              Text(
                text = "// ${file.fileName} (${file.language})",
                style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontFamily = FontFamily.Monospace)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.6f), CyberTokens.shapeSmall)
                  .padding(10.dp)
              ) {
                Text(
                  text = file.content,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge)) }
  }
}

/**
 * Creator Studio & Persona Craft Screen.
 */
@Composable
fun CreatorScreen(
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val personalityEngine = agentController?.aiEngine?.personalityEngine
  val creatorEngine = agentController?.aiEngine?.creatorEngine

  val currentArchetype by personalityEngine?.currentArchetype?.collectAsState()
    ?: remember { mutableStateOf(PersonalityArchetype.PERSONAL_ASSISTANT) }

  var customDirectives by remember { mutableStateOf(personalityEngine?.customDirectives?.value ?: "") }
  var savedStatus by remember { mutableStateOf(false) }

  var websitePrompt by remember { mutableStateOf("") }
  var isGeneratingWeb by remember { mutableStateOf(false) }
  var webProjectOutput by remember { mutableStateOf<CreatorOutput?>(null) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SYNTHETIC PERSONA // CREATOR STUDIO",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
        )
        StatusBadge(text = currentArchetype.title, indicatorColor = CyanNeon, isPulsing = false)
      }
    }

    // Personality Archetype Selector
    item {
      FuturisticCard(headerText = "Personality Archetype", badgeText = "System Behavior") {
        PersonalityArchetype.values().forEach { arch ->
          val isSelected = arch == currentArchetype
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { personalityEngine?.setArchetype(arch) }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyanNeon else Color.Transparent)
                .border(CyberTokens.borderHairline, if (isSelected) CyanNeon else TextTertiary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = arch.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = if (isSelected) CyanNeon else TextPrimary,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              )
              Text(
                text = arch.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
              )
            }
          }
        }
      }
    }

    // Custom System Directives
    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("OPERATOR SYSTEM DIRECTIVES", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = customDirectives,
            onValueChange = {
              customDirectives = it
              savedStatus = false
            },
            placeholder = { Text("Add custom guidelines (e.g. Always include type definitions, avoid excessive apologies)...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("custom_directives_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = Color(0x3300F0FF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = CyberTokens.shapeSmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          FuturisticButton(
            text = "Save Directives",
            onClick = {
              personalityEngine?.setCustomDirectives(customDirectives)
              savedStatus = true
            },
            isPrimary = true,
            modifier = Modifier.fillMaxWidth().testTag("save_directives_button")
          )
          if (savedStatus) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Directives active for all incoming interactions.", style = MaterialTheme.typography.labelSmall.copy(color = ElectricBlue))
          }
        }
      }
    }

    // Website / App Creator Project Generator
    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text("FULL-STACK PROJECT GENERATOR", style = MaterialTheme.typography.labelSmall.copy(color = HologramViolet, fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = websitePrompt,
            onValueChange = { websitePrompt = it },
            placeholder = { Text("Describe a website or web app to build (e.g. Cyberpunk Crypto Dashboard with live charts)...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
            modifier = Modifier.fillMaxWidth().testTag("web_project_prompt_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = HologramViolet,
              unfocusedBorderColor = Color(0x33BD00FF),
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = CyberTokens.shapeSmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          FuturisticButton(
            text = if (isGeneratingWeb) "Generating Full Web Project..." else "Build Web Project",
            onClick = {
              if (websitePrompt.isNotBlank() && creatorEngine != null) {
                isGeneratingWeb = true
                scope.launch {
                  webProjectOutput = creatorEngine.generateWebsiteProject(websitePrompt)
                  isGeneratingWeb = false
                }
              }
            },
            isPrimary = true,
            modifier = Modifier.fillMaxWidth().testTag("generate_web_button")
          )
        }
      }
    }

    if (webProjectOutput != null) {
      val out = webProjectOutput!!
      item {
        FuturisticCard(headerText = out.title, badgeText = "Multi-file Project") {
          out.files.forEach { file ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
              Text(
                text = "File: ${file.fileName}",
                style = MaterialTheme.typography.labelSmall.copy(color = HologramViolet, fontFamily = FontFamily.Monospace)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.6f), CyberTokens.shapeSmall)
                  .padding(8.dp)
              ) {
                Text(
                  text = file.content.take(300) + if (file.content.length > 300) "\n// ... [${file.content.length} chars total]" else "",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    fontSize = 11.sp
                  )
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge)) }
  }
}
