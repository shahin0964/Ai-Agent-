package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.core.agent.AgentController
import com.example.core.capability.ActionConfirmationMode
import com.example.core.capability.CapabilityCategory
import com.example.core.capability.CapabilityRegistry
import com.example.core.capability.DeviceCapability
import com.example.core.capability.SettingsDestination
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated Permission Center Screen Architecture.
 * Strictly verifies Android permissions and settings without fake claims.
 * Allows toggling agent capability policies and confirmation modes.
 */
@Composable
fun PermissionCenterScreen(
  onBack: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Trigger recomposition on lifecycle resume (e.g. returning from system settings)
  var resumeCounter by remember { mutableIntStateOf(0) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        resumeCounter++
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val enabledMap = if (agentController != null) {
    agentController.policy.enabledCapabilities.collectAsState().value
  } else {
    emptyMap()
  }

  val currentConfirmationMode = if (agentController != null) {
    agentController.policy.confirmationMode.collectAsState().value
  } else {
    ActionConfirmationMode.NORMAL
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) {
    resumeCounter++
  }

  val allCapabilities = CapabilityRegistry.allCapabilities
  val groupedCapabilities = remember(resumeCounter) {
    allCapabilities.groupBy { it.category }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    // Back navigation header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("permission_center_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyanNeon
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "SECURITY AUDIT // PERMISSIONS",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            )
          )
          Text(
            text = "Android Platform Capability Matrix",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
        }
      }
    }

    // Android Security Contract Card
    item {
      FuturisticCard(headerText = "Android Security Policy", badgeText = "Official Contract") {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Actions are executed strictly through explicit, user-authorized Android APIs. No hidden, elevated, or unauthorized device control.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FuturisticButton(
          text = "Open System App Settings",
          onClick = {
            openSystemSettings(context, SettingsDestination.APP_DETAILS)
          },
          isPrimary = false,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // Action Confirmation Mode Selector
    item {
      FuturisticCard(headerText = "Action Confirmation Policy", badgeText = currentConfirmationMode.label) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Controls whether the Agent requires explicit operator confirmation dialogs before executing actions.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )

          ActionConfirmationMode.values().forEach { mode ->
            val isSelected = mode == currentConfirmationMode
            GlassPanel(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  agentController?.policy?.setConfirmationMode(mode)
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mode.label.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) CyanNeon else TextPrimary
                    )
                  )
                  Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TextTertiary,
                      fontSize = 11.sp
                    )
                  )
                }
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    // Capabilities grouped by Category
    CapabilityCategory.values().forEach { category ->
      val itemsInCategory = groupedCapabilities[category] ?: emptyList()
      if (itemsInCategory.isNotEmpty()) {
        item {
          Text(
            text = category.title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            ),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
          )
        }

        items(itemsInCategory, key = { it.id }) { capability ->
          val isEnabledInAgent = enabledMap[capability.type] ?: true
          val isGrantedInAndroid = capability.isCurrentlyGranted(context)

          GlassPanel(
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = capability.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                  )
                  Text(
                    text = capability.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TextSecondary,
                      fontSize = 11.sp
                    )
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                  checked = isEnabledInAgent,
                  onCheckedChange = { checked ->
                    agentController?.policy?.setCapabilityEnabled(capability.type, checked)
                  },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanNeon,
                    checkedTrackColor = ElectricBlue.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = StatusOffline.copy(alpha = 0.3f)
                  )
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Android OS: ",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                  )
                  StatusBadge(
                    text = if (isGrantedInAndroid) "GRANTED" else "NOT GRANTED",
                    indicatorColor = if (isGrantedInAndroid) StatusReady else StatusWarning,
                    isPulsing = false
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Agent Policy: ",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                  )
                  StatusBadge(
                    text = if (isEnabledInAgent) "ENABLED" else "DISABLED",
                    indicatorColor = if (isEnabledInAgent) CyanNeon else StatusAlert,
                    isPulsing = false
                  )
                }
              }

              if (!isEnabledInAgent && isGrantedInAndroid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Notice: Granted by Android OS, but Agent access is disabled in in-app policy.",
                  style = MaterialTheme.typography.labelSmall.copy(color = StatusWarning, fontSize = 10.sp)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Action buttons (request permission or open system settings)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = capability.stateNote,
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    fontSize = 10.sp
                  ),
                  modifier = Modifier.weight(1f)
                )

                if (capability.requiredPermissions.isNotEmpty() && !isGrantedInAndroid) {
                  FuturisticButton(
                    text = "Request Permission",
                    onClick = {
                      permissionLauncher.launch(capability.requiredPermissions.toTypedArray())
                    },
                    isPrimary = true,
                    modifier = Modifier.padding(start = 8.dp)
                  )
                } else if (capability.settingsDestination != SettingsDestination.NONE) {
                  Row(
                    modifier = Modifier
                      .clickable {
                        openSystemSettings(context, capability.settingsDestination)
                      }
                      .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "System Settings",
                      style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontSize = 10.sp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                      contentDescription = null,
                      tint = CyanNeon,
                      modifier = Modifier.size(12.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
    }
  }
}

private fun openSystemSettings(context: Context, destination: SettingsDestination) {
  val intent = when (destination) {
    SettingsDestination.APP_DETAILS -> {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
    }
    SettingsDestination.NOTIFICATION_LISTENER -> {
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    SettingsDestination.ACCESSIBILITY -> {
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    SettingsDestination.BLUETOOTH -> {
      Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    }
    SettingsDestination.LOCATION -> {
      Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }
    SettingsDestination.NONE -> {
      Intent(Settings.ACTION_SETTINGS)
    }
  }
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(fallback)
  }
}
