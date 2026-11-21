package com.example.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.AppContainer
import com.example.data.ai.ScreenScanOutcome
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "QuickCaptureService"
private const val CHANNEL_ID = "quick_champion_scan_channel"
private const val NOTIFICATION_ID = 4107

class QuickCaptureService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  private var captureThread: HandlerThread? = null
  private var captureHandler: Handler? = null

  private var displayWidth = 1080
  private var displayHeight = 1920
  private var displayDensity = DisplayMetrics.DENSITY_HIGH

  private val captureRequested = AtomicBoolean(false)
  private var projectionCallback: MediaProjection.Callback? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    resolveDisplayMetrics()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START_PROJECTION -> {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = extractParcelableIntent(intent, EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
          stopSelf()
          return START_NOT_STICKY
        }
        promoteToForeground(
          title = "Rift Quick Capture พร้อมใช้งาน",
          body = "เปิดเกม Wild Rift แล้วกด 'แคป & สแกน' เพื่อส่งเข้า 1v5, Draft และ AI Coach อัตโนมัติ",
        )
        startMediaProjection(resultCode, resultData)
      }
      ACTION_CAPTURE_ONCE -> {
        val delayMs = intent.getLongExtra(EXTRA_DELAY_MS, 450L)
        if (mediaProjection == null || virtualDisplay == null) {
          AppContainer.getInstance(applicationContext)
            .quickScanCoordinator
            .reportCaptureError("กรุณาเปิด Quick Capture (อนุญาตจับภาพหน้าจอ) ก่อนทำการแคปเร็ว")
          return START_NOT_STICKY
        }
        serviceScope.launch {
          if (delayMs > 0L) delay(delayMs)
          captureRequested.set(true)
        }
      }
      ACTION_STOP_PROJECTION -> {
        teardownProjection()
        stopSelf()
      }
    }
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    teardownProjection()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun startMediaProjection(resultCode: Int, data: Intent) {
    teardownProjection(updateCoordinator = false)

    val thread = HandlerThread("QuickCaptureThread").apply { start() }
    captureThread = thread
    captureHandler = Handler(thread.looper)

    val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val projection =
      try {
        mgr.getMediaProjection(resultCode, data)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to obtain MediaProjection", e)
        null
      }

    if (projection == null) {
      AppContainer.getInstance(applicationContext)
        .quickScanCoordinator
        .reportCaptureError("ไม่สามารถเริ่มจับภาพหน้าจอได้ กรุณาลองอนุญาตใหม่อีกครั้ง")
      stopSelf()
      return
    }

    val callback =
      object : MediaProjection.Callback() {
        override fun onStop() {
          Log.d(TAG, "MediaProjection stopped by system or user")
          Handler(Looper.getMainLooper()).post {
            teardownProjection()
            stopSelf()
          }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
          if (width > 0 && height > 0 && (width != displayWidth || height != displayHeight)) {
            displayWidth = width
            displayHeight = height
            recreateImageReaderAndResizeDisplay()
          }
        }
      }
    projectionCallback = callback
    projection.registerCallback(callback, captureHandler)
    mediaProjection = projection

    createImageReaderAndVirtualDisplay(projection)
    AppContainer.getInstance(applicationContext).quickScanCoordinator.setQuickCaptureActive(true)
  }

  private fun createImageReaderAndVirtualDisplay(projection: MediaProjection) {
    resolveDisplayMetrics()
    val reader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)
    reader.setOnImageAvailableListener(
      { ir ->
        var image: Image? = null
        try {
          image = ir.acquireLatestImage()
          if (image != null && captureRequested.compareAndSet(true, false)) {
            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
              saveLatestCaptureToCache(bitmap)
              dispatchCapturedBitmap(bitmap)
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error acquiring capture image", e)
        } finally {
          image?.close()
        }
      },
      captureHandler,
    )
    imageReader = reader
    virtualDisplay =
      projection.createVirtualDisplay(
        "RiftQuickCapture",
        displayWidth,
        displayHeight,
        displayDensity,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        reader.surface,
        null,
        captureHandler,
      )
  }

  private fun recreateImageReaderAndResizeDisplay() {
    val projection = mediaProjection ?: return
    val oldReader = imageReader
    val vd = virtualDisplay
    if (vd != null) {
      val reader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)
      reader.setOnImageAvailableListener(
        { ir ->
          var image: Image? = null
          try {
            image = ir.acquireLatestImage()
            if (image != null && captureRequested.compareAndSet(true, false)) {
              val bitmap = imageToBitmap(image)
              if (bitmap != null) {
                saveLatestCaptureToCache(bitmap)
                dispatchCapturedBitmap(bitmap)
              }
            }
          } catch (e: Exception) {
            Log.e(TAG, "Error acquiring resized capture image", e)
          } finally {
            image?.close()
          }
        },
        captureHandler,
      )
      imageReader = reader
      vd.resize(displayWidth, displayHeight, displayDensity)
      vd.surface = reader.surface
      oldReader?.close()
    } else {
      createImageReaderAndVirtualDisplay(projection)
    }
  }

  private fun dispatchCapturedBitmap(bitmap: Bitmap) {
    serviceScope.launch {
      updateNotification(
        title = "กำลังสแกนแชมเปี้ยนจากหน้าจอ...",
        body = "กำลังวิเคราะห์ฝั่งเราและฝั่งตรงข้ามส่งเข้า 1v5, Draft, AI Coach",
      )
      val container = AppContainer.getInstance(applicationContext)
      val outcome = container.quickScanCoordinator.scanCapturedBitmap(bitmap, openSheet = true)
      when (outcome) {
        is ScreenScanOutcome.Success -> {
          val res = outcome.result
          val msg =
            "พบฝั่งเรา ${res.allies.size} • ศัตรู ${res.enemies.size}" +
              if (res.skippedUnpickedCount > 0) " (ข้ามช่องว่าง ${res.skippedUnpickedCount})" else ""
          updateNotification(
            title = "ส่งข้อมูลเข้า 1v5 / Draft / AI Coach แล้ว",
            body = "$msg — แตะเพื่อเปิดดูวิเคราะห์ทันที",
          )
        }
        is ScreenScanOutcome.Failure -> {
          updateNotification(
            title = "สแกนภาพไม่สำเร็จ",
            body = outcome.message.take(120),
          )
        }
      }
    }
  }

  private fun imageToBitmap(image: Image): Bitmap? {
    val planes = image.planes
    if (planes.isEmpty()) return null
    val plane = planes[0]
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * image.width
    val rawBitmap =
      Bitmap.createBitmap(
        image.width + rowPadding / pixelStride,
        image.height,
        Bitmap.Config.ARGB_8888,
      )
    rawBitmap.copyPixelsFromBuffer(buffer)
    return if (rawBitmap.width != image.width) {
      val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
      rawBitmap.recycle()
      cropped
    } else {
      rawBitmap
    }
  }

  private fun saveLatestCaptureToCache(bitmap: Bitmap) {
    try {
      val dir = File(cacheDir, "captures").apply { mkdirs() }
      val file = File(dir, "latest_capture.jpg")
      FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out) }
    } catch (e: Exception) {
      Log.w(TAG, "Unable to cache capture thumbnail", e)
    }
  }

  private fun resolveDisplayMetrics() {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bounds = wm.maximumWindowMetrics.bounds
      displayWidth = bounds.width().coerceAtLeast(360)
      displayHeight = bounds.height().coerceAtLeast(360)
      displayDensity = resources.configuration.densityDpi.coerceAtLeast(160)
    } else {
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      wm.defaultDisplay.getRealMetrics(metrics)
      displayWidth = metrics.widthPixels.coerceAtLeast(360)
      displayHeight = metrics.heightPixels.coerceAtLeast(360)
      displayDensity = metrics.densityDpi.coerceAtLeast(160)
    }
  }

  private fun teardownProjection(updateCoordinator: Boolean = true) {
    captureRequested.set(false)
    virtualDisplay?.release()
    virtualDisplay = null
    imageReader?.close()
    imageReader = null
    projectionCallback?.let { cb ->
      try {
        mediaProjection?.unregisterCallback(cb)
      } catch (_: Exception) {
      }
    }
    projectionCallback = null
    mediaProjection?.stop()
    mediaProjection = null
    captureThread?.quitSafely()
    captureThread = null
    captureHandler = null
    if (updateCoordinator) {
      AppContainer.getInstance(applicationContext).quickScanCoordinator.setQuickCaptureActive(false)
    }
  }

  private fun promoteToForeground(title: String, body: String) {
    val notification = buildNotification(title, body)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun updateNotification(title: String, body: String) {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, buildNotification(title, body))
  }

  private fun buildNotification(title: String, body: String): Notification {
    val openAppIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.EXTRA_OPEN_QUICK_SCAN, true)
      }
    val openAppPi =
      PendingIntent.getActivity(
        this,
        101,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val captureIntent =
      Intent(this, QuickCaptureActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val capturePi =
      PendingIntent.getActivity(
        this,
        102,
        captureIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent = Intent(this, QuickCaptureService::class.java).setAction(ACTION_STOP_PROJECTION)
    val stopPi =
      PendingIntent.getService(
        this,
        103,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openAppPi)
      .addAction(android.R.drawable.ic_menu_camera, "แคป & สแกน", capturePi)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ปิดโหมด", stopPi)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            getString(R.string.quick_capture_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply { description = getString(R.string.quick_capture_channel_desc) }
      nm.createNotificationChannel(channel)
    }
  }

  private fun extractParcelableIntent(intent: Intent, key: String): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(key, Intent::class.java)
    } else {
      @Suppress("DEPRECATION") intent.getParcelableExtra(key)
    }

  companion object {
    const val ACTION_START_PROJECTION = "com.example.capture.START_PROJECTION"
    const val ACTION_CAPTURE_ONCE = "com.example.capture.CAPTURE_ONCE"
    const val ACTION_STOP_PROJECTION = "com.example.capture.STOP_PROJECTION"
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_DELAY_MS = "extra_delay_ms"

    fun startProjection(context: Context, resultCode: Int, data: Intent) {
      val intent =
        Intent(context, QuickCaptureService::class.java).apply {
          action = ACTION_START_PROJECTION
          putExtra(EXTRA_RESULT_CODE, resultCode)
          putExtra(EXTRA_RESULT_DATA, data)
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun triggerCapture(context: Context, delayMs: Long = 450L) {
      val intent =
        Intent(context, QuickCaptureService::class.java).apply {
          action = ACTION_CAPTURE_ONCE
          putExtra(EXTRA_DELAY_MS, delayMs)
        }
      context.startService(intent)
    }

    fun stopProjection(context: Context) {
      val intent =
        Intent(context, QuickCaptureService::class.java).setAction(ACTION_STOP_PROJECTION)
      context.startService(intent)
    }
  }
}
