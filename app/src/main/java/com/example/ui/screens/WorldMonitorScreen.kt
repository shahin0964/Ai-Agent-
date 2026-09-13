package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.ai.research.ResearchResult
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.FuturisticHeader
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.activeTheme
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class MonitorCategory(val displayName: String, val searchQuery: String) {
  GLOBAL("Global Events", "latest global news and world events"),
  TECH("Technology & AI", "artificial intelligence latest developments technology breakthroughs"),
  SCIENCE("Science & Space", "space exploration astronomical discoveries scientific breakthroughs"),
  CYBER("Cybersecurity", "cybersecurity intelligence vulnerabilities digital privacy"),
  MARKETS("Global Economy", "global markets economic indicators technology finance")
}

@Composable
fun WorldMonitorScreen(
  agentController: AgentController,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val theme = activeTheme
  val scope = rememberCoroutineScope()
  val researchEngine = agentController.aiEngine?.researchEngine

  var selectedCategory by remember { mutableStateOf(MonitorCategory.GLOBAL) }
  var currentResult by remember { mutableStateOf<ResearchResult?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Load intelligence on category change
  fun refreshIntelligence(category: MonitorCategory) {
    if (researchEngine == null) {
      errorMessage = "Research Intelligence engine is unavailable."
      return
    }
    isLoading = true
    errorMessage = null
    scope.launch {
      try {
        val res = researchEngine.conductResearch(
          query = category.searchQuery
        )
        currentResult = res
      } catch (e: Exception) {
        errorMessage = "Network or research error: ${e.message}"
      } finally {
        isLoading = false
      }
    }
  }

  LaunchedEffect(selectedCategory) {
    refreshIntelligence(selectedCategory)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("world_monitor_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = theme.primary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "WORLD MONITOR",
            style = MaterialTheme.typography.titleMedium.copy(
              color = theme.primary,
              letterSpacing = 2.sp
            )
          )
          Text(
            text = "Real-Time Global Intelligence Radar",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
          )
        }
      }

      IconButton(
        onClick = { refreshIntelligence(selectedCategory) },
        enabled = !isLoading,
        modifier = Modifier.testTag("world_monitor_refresh_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh",
          tint = if (isLoading) TextTertiary else theme.primary
        )
      }
    }

    // World Radar Vector Canvas
    WorldRadarCanvas(
      primaryColor = theme.primary,
      secondaryColor = theme.secondary,
      accentColor = theme.accent,
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF070B16))
        .border(1.dp, theme.glassBorder, RoundedCornerShape(12.dp))
    )

    // Category Selector Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MonitorCategory.entries.forEach { cat ->
        val isSelected = cat == selectedCategory
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) theme.primary.copy(alpha = 0.2f) else Color(0xFF0F172A))
            .border(
              1.dp,
              if (isSelected) theme.primary else Color(0xFF334155),
              RoundedCornerShape(8.dp)
            )
            .clickable { selectedCategory = cat }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = cat.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) theme.primary else TextSecondary,
              fontSize = 11.sp
            )
          )
        }
      }
    }

    // Intelligence Feed Content
    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(32.dp))
          Text(
            text = "Scanning global feeds for ${selectedCategory.displayName}...",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
        }
      }
    } else if (errorMessage != null) {
      FuturisticCard(
        headerText = "FEED STATUS",
        badgeText = "OFFLINE / ERROR",
        badgeColor = StatusWarning
      ) {
        Text(
          text = errorMessage ?: "Unknown error retrieving global telemetry.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    } else {
      currentResult?.let { res ->
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
        ) {
          // Synthesis Summary Card
          item {
            FuturisticCard(
              headerText = "GLOBAL INTELLIGENCE BRIEFING",
              badgeText = "${res.sources.size} SOURCES VERIFIED",
              badgeColor = StatusReady
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = res.synthesizedAnswer,
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                )
                Text(
                  text = "Latency: ${res.searchLatencyMs}ms • Confidence: ${res.confidence.label}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    fontSize = 10.sp
                  )
                )
              }
            }
          }

          // Verified Citations & Sources
          if (res.sources.isNotEmpty()) {
            item {
              Text(
                text = "VERIFIED KNOWLEDGE CITATIONS",
                style = MaterialTheme.typography.titleSmall.copy(
                  color = theme.primary,
                  letterSpacing = 1.sp
                )
              )
            }

            items(res.sources) { source ->
              FuturisticCard(
                headerText = source.title,
                badgeText = source.sourceDomain.ifBlank { "Web Source" },
                badgeColor = theme.secondary
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = source.snippet,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                  )
                  if (source.url.isNotBlank()) {
                    Text(
                      text = source.url,
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = theme.secondary,
                        fontSize = 10.sp
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * High-performance, lightweight Canvas Radar Vector Animation.
 */
@Composable
fun WorldRadarCanvas(
  primaryColor: Color,
  secondaryColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "RadarSweep")
  val sweepAngle by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RadarSweepAngle"
  )

  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = (size.height / 2f) * 0.85f

    // 1. Concentric Range Rings
    for (i in 1..3) {
      val r = maxRadius * (i / 3f)
      drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = r,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )
    }

    // 2. Latitude / Longitude Crosshairs
    drawLine(
      color = primaryColor.copy(alpha = 0.2f),
      start = Offset(center.x - maxRadius, center.y),
      end = Offset(center.x + maxRadius, center.y),
      strokeWidth = 1.dp.toPx()
    )
    drawLine(
      color = primaryColor.copy(alpha = 0.2f),
      start = Offset(center.x, center.y - maxRadius),
      end = Offset(center.x, center.y + maxRadius),
      strokeWidth = 1.dp.toPx()
    )

    // 3. Rotating Radar Sweep Beam
    val rad = Math.toRadians(sweepAngle.toDouble())
    val endX = center.x + (maxRadius * cos(rad)).toFloat()
    val endY = center.y + (maxRadius * sin(rad)).toFloat()

    drawLine(
      color = primaryColor.copy(alpha = 0.7f),
      start = center,
      end = Offset(endX, endY),
      strokeWidth = 2.dp.toPx()
    )

    // 4. Global Geographic Indicator Beacons
    val beaconOffsets = listOf(
      Offset(center.x - maxRadius * 0.5f, center.y - maxRadius * 0.3f), // Americas
      Offset(center.x + maxRadius * 0.1f, center.y - maxRadius * 0.4f), // Europe
      Offset(center.x + maxRadius * 0.6f, center.y + maxRadius * 0.1f), // Asia Pacific
      Offset(center.x - maxRadius * 0.2f, center.y + maxRadius * 0.5f)  // Africa/South
    )

    beaconOffsets.forEach { beacon ->
      drawCircle(
        color = accentColor.copy(alpha = 0.8f),
        radius = 3.dp.toPx(),
        center = beacon
      )
      drawCircle(
        color = accentColor.copy(alpha = 0.25f),
        radius = 7.dp.toPx(),
        center = beacon
      )
    }
  }
}
