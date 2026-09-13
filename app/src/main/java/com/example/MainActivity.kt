package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.agent.RealAgentController
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.ui.theme.AppThemeState.init(this)
    enableEdgeToEdge()
    setContent {
      val agentController = remember { RealAgentController(this) }
      androidx.compose.runtime.DisposableEffect(agentController) {
        onDispose {
          agentController.release()
        }
      }
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = com.example.ui.theme.activeTheme.background
        ) {
          MainAppScreen(agentController = agentController)
        }
      }
    }
  }
}


