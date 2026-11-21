package com.example.capture

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.MainActivity
import com.example.core.AppContainer

/**
 * Translucent Activity used by the notification action / Quick Settings tile to collapse the
 * system shade without violating Android 12+ notification trampoline restrictions, then trigger
 * delayed screen capture.
 */
class QuickCaptureActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val container = AppContainer.getInstance(applicationContext)
    if (container.quickScanCoordinator.uiState.value.isQuickCaptureActive) {
      QuickCaptureService.triggerCapture(applicationContext, delayMs = 450L)
    } else {
      val openIntent =
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra(MainActivity.EXTRA_OPEN_QUICK_SCAN, true)
        }
      startActivity(openIntent)
    }
    finish()
  }
}
