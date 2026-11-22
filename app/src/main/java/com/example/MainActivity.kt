package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.AppContainer
import com.example.core.configureAppCheck
import com.example.ui.navigation.RiftCompanionApp

class MainActivity : ComponentActivity() {
  private lateinit var appContainer: AppContainer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before any UI, ViewModels, or AI calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    appContainer = AppContainer.getInstance(applicationContext)
    handleIncomingIntent(intent)

    setContent { RiftCompanionApp(appContainer = appContainer) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (::appContainer.isInitialized) {
      handleIncomingIntent(intent)
    }
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
      val imageUri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
      if (imageUri != null) {
        appContainer.quickScanCoordinator.scanUriAsync(imageUri, openSheet = true)
      }
    } else if (intent.getBooleanExtra(EXTRA_OPEN_LIVE_COACH, false)) {
      appContainer.quickScanCoordinator.requestOpenLiveCoach()
    } else if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_SCAN, false)) {
      appContainer.quickScanCoordinator.openSheet()
    }
  }

  companion object {
    const val EXTRA_OPEN_QUICK_SCAN = "extra_open_quick_scan"
    const val EXTRA_OPEN_LIVE_COACH = "extra_open_live_coach"
  }
}
