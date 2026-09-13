package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.navigation.SidebarDestinations
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Futuristic Slide-out Navigation Drawer (Sidebar).
 * Features translucent glass surface, subtle glowing border,
 * clear iconography, smooth transition, and selected-state indicators.
 */
@Composable
fun FuturisticSidebar(
  isOpen: Boolean,
  currentScreen: Screen,
  onNavigate: (Screen) -> Unit,
  onClose: () -> Unit
) {
  val theme = com.example.ui.theme.activeTheme

  AnimatedVisibility(
    visible = isOpen,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    // Backdrop overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0x99000000))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClose
        )
    )
  }

  AnimatedVisibility(
    visible = isOpen,
    enter = slideInHorizontally(initialOffsetX = { -it }),
    exit = slideOutHorizontally(targetOffsetX = { -it })
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(CyberTokens.sidebarWidth)
        .background(theme.surface)
        .border(
          CyberTokens.borderHairline,
          theme.primary.copy(alpha = 0.35f)
        )
        .statusBarsPadding()
        .testTag("futuristic_sidebar")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(CyberTokens.spaceLarge)
      ) {
        // Sidebar Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(theme.primary.copy(alpha = 0.15f))
                .border(CyberTokens.borderHairline, theme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = theme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "AEGIS AI",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.5.sp,
                  color = TextPrimary
                )
              )
              Text(
                text = "COMMAND MATRIX",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  color = theme.primary,
                  letterSpacing = 1.sp
                )
              )
            }
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("sidebar_close_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Sidebar",
              tint = theme.primary
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Categories
        Text(
          text = "CORE MODULES",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextTertiary
          ),
          modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        SidebarDestinations.forEach { dest ->
          val isSelected = currentScreen == dest
          SidebarItem(
            screen = dest,
            isSelected = isSelected,
            onClick = {
              onNavigate(dest)
              onClose()
            }
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Telemetry Tag
        Surface(
          shape = CyberTokens.shapeSmall,
          color = theme.primary.copy(alpha = 0.12f),
          border = androidx.compose.foundation.BorderStroke(CyberTokens.borderHairline, theme.primary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(theme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "System: Operational // Active",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = TextSecondary
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SidebarItem(
  screen: Screen,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val theme = com.example.ui.theme.activeTheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(CyberTokens.shapeSmall)
      .background(
        if (isSelected) theme.primary.copy(alpha = 0.18f) else Color.Transparent
      )
      .border(
        CyberTokens.borderHairline,
        if (isSelected) theme.primary.copy(alpha = 0.6f) else Color.Transparent,
        CyberTokens.shapeSmall
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("sidebar_item_${screen.route.replace("/", "_")}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = screen.icon,
        contentDescription = null,
        tint = if (isSelected) theme.primary else TextSecondary,
        modifier = Modifier.size(20.dp)
      )

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = screen.title,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          color = if (isSelected) theme.primary else TextPrimary
        )
      )

      if (isSelected) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(theme.primary)
        )
      }
    }
  }
}
