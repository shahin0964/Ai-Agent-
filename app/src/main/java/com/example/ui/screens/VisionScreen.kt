package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.agent.AgentController
import com.example.core.ai.vision.VisionAnalysisResult
import com.example.core.ai.vision.VisionState
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
 * Dedicated Vision AI screen architecture.
 * Supports Zero-Permission Android Photo Picker, Image Selection,
 * Visual Q&A via Multimodal AI Provider, and OCR scene comprehension.
 */
@Composable
fun VisionScreen(
  onNavigateToAiProviders: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()

  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var visualPrompt by remember { mutableStateOf("") }
  var visionState by remember { mutableStateOf(VisionState.NO_IMAGE) }
  var analysisResult by remember { mutableStateOf<VisionAnalysisResult?>(null) }
  var statusMessage by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedImageUri = uri
      visionState = VisionState.IMAGE_SELECTED
      statusMessage = null
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header Telemetry
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = CyberTokens.spaceMedium),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "OPTICAL SUBSYSTEM // VISION AI",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
      val isVisionReady = agentController?.aiEngine?.let {
        it.geminiProvider.isConfigured || it.openAiProvider.isConfigured || it.anthropicProvider.isConfigured
      } ?: false

      StatusBadge(
        text = if (isVisionReady) "VISION PIPELINE: ONLINE" else "VISION: NO KEY CONFIGURED",
        indicatorColor = if (isVisionReady) StatusReady else StatusOffline,
        isPulsing = isVisionReady
      )
    }

    // Viewport Aperture / Image Display
    GlassPanel(
      modifier = Modifier
        .fillMaxWidth()
        .height(if (selectedImageUri != null) 280.dp else 210.dp)
    ) {
      if (selectedImageUri != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          AsyncImage(
            model = selectedImageUri,
            contentDescription = "Selected Optical Subject",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          // Clear button
          IconButton(
            onClick = {
              selectedImageUri = null
              visionState = VisionState.NO_IMAGE
              analysisResult = null
            },
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .background(Color.Black.copy(alpha = 0.6f), CircleShape)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Clear Image", tint = Color.White)
          }
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(CyberTokens.spaceLarge),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(Color(0x3300F0FF))
              .border(CyberTokens.borderNormal, CyanNeon.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "OPTICAL SENSOR VIEWPORT",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              letterSpacing = 1.sp
            )
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Select an image from device storage using the zero-permission Android Photo Picker.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextSecondary,
              textAlign = TextAlign.Center
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))

    // Action Controls: Pick Image Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FuturisticButton(
        text = if (selectedImageUri == null) "Select Image (Photo Picker)" else "Change Image",
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        isPrimary = selectedImageUri == null,
        modifier = Modifier.weight(1f).testTag("select_image_button")
      )
    }

    if (selectedImageUri != null) {
      Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))

      // Prompt input field
      OutlinedTextField(
        value = visualPrompt,
        onValueChange = { visualPrompt = it },
        placeholder = {
          Text("Ask anything about this image (or leave blank for full scene analysis)...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary))
        },
        modifier = Modifier.fillMaxWidth().testTag("visual_prompt_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = Color(0x3300F0FF),
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = CyberTokens.shapeSmall
      )

      Spacer(modifier = Modifier.height(CyberTokens.spaceSmall))

      FuturisticButton(
        text = if (visionState == VisionState.ANALYZING) "Analyzing..." else "Analyze Image",
        onClick = {
          val uri = selectedImageUri ?: return@FuturisticButton
          val engine = agentController?.aiEngine?.visionEngine
          if (engine == null) {
            statusMessage = "Vision engine unavailable."
            return@FuturisticButton
          }

          visionState = VisionState.ANALYZING
          scope.launch {
            val result = engine.analyzeImage(uri, visualPrompt)
            analysisResult = result
            visionState = VisionState.RESULT
          }
        },
        isPrimary = true,
        modifier = Modifier.fillMaxWidth().testTag("analyze_image_button")
      )
    }

    // Loading / Result view
    if (visionState == VisionState.ANALYZING) {
      Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Running optical neural inference...", style = MaterialTheme.typography.bodySmall.copy(color = CyanNeon))
      }
    }

    if (analysisResult != null) {
      Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))
      FuturisticCard(
        headerText = "OPTICAL REASONING REPORT",
        badgeText = analysisResult?.modelUsed ?: "Multimodal"
      ) {
        Text(
          text = analysisResult?.analysisText ?: "",
          style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Latency: ${analysisResult?.latencyMs}ms",
          style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
        )
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

    // Hardware & AI Provider Setup Card
    FuturisticCard(
      onClick = onNavigateToAiProviders,
      headerText = "Hardware & AI Provider Setup",
      badgeText = "Vision Settings"
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Configure Vision Provider",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
          Text(
            text = "Configure Gemini 2.5 Flash, GPT-4o, or Claude 3.5 Sonnet credentials in AI Providers.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
  }
}
