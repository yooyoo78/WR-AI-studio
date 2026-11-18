package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.AppContainer
import com.example.core.configureAppCheck
import com.example.ui.navigation.RiftCompanionApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before any UI, ViewModels, or AI calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    val appContainer = AppContainer(applicationContext)

    setContent { RiftCompanionApp(appContainer = appContainer) }
  }
}
