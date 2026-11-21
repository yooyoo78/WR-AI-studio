package com.example.capture

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.core.AppContainer

class RiftQuickScanTileService : TileService() {
  override fun onStartListening() {
    super.onStartListening()
    val tile = qsTile ?: return
    val isActive = AppContainer.getInstance(applicationContext).quickScanCoordinator.uiState.value.isQuickCaptureActive
    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.label = getString(com.example.R.string.quick_scan_tile_label)
    tile.updateTile()
  }

  override fun onClick() {
    super.onClick()
    val intent =
      Intent(this, QuickCaptureActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val pi =
        PendingIntent.getActivity(
          this,
          201,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      startActivityAndCollapse(pi)
    } else {
      @Suppress("DEPRECATION")
      startActivityAndCollapse(intent)
    }
  }
}
