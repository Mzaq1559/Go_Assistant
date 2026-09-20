package com.goassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "com.goassistant/overlay"
    private val reqOverlay = 1001
    private val reqProjection = 1002
    private var projectionResult: MethodChannel.Result? = null
    private var overlayResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "hasOverlayPermission" -> result.success(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
                    )
                    "requestOverlayPermission" -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) result.success(true)
                        else {
                            overlayResult = result
                            startActivityForResult(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                                reqOverlay
                            )
                        }
                    }
                    "requestScreenCapture" -> {
                        if (projectionResult != null) {
                            result.error("IN_PROGRESS", "Screen capture permission request already in progress", null)
                        } else {
                            projectionResult = result
                            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            startActivityForResult(mgr.createScreenCaptureIntent(), reqProjection)
                        }
                    }
                    "startOverlay" -> {
                        val apiKey = call.argument<String>("apiKey")?.trim().orEmpty()
                        if (apiKey.isEmpty()) {
                            result.error("INVALID_KEY", "Claude API key is empty", null)
                            return@setMethodCallHandler
                        }
                        val intent = Intent(this, OverlayService::class.java).apply {
                            putExtra("apiKey", apiKey)
                            putExtra("turn", call.argument<String>("turn") ?: "black")
                            putExtra("boardSize", call.argument<Int>("boardSize") ?: 19)
                        }
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                            else startService(intent)
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("SERVICE_START_FAILED", e.message, null)
                        }
                    }
                    "stopOverlay" -> {
                        stopService(Intent(this, OverlayService::class.java))
                        result.success(true)
                    }
                    "isOverlayRunning" -> result.success(OverlayService.isRunning)
                    else -> result.notImplemented()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            reqOverlay -> {
                val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
                overlayResult?.success(granted)
                overlayResult = null
            }
            reqProjection -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    OverlayService.projectionResultCode = resultCode
                    OverlayService.projectionData = data
                    projectionResult?.success(true)
                } else projectionResult?.success(false)
                projectionResult = null
            }
        }
    }

    override fun onDestroy() {
        projectionResult?.success(false)
        projectionResult = null
        overlayResult?.success(false)
        overlayResult = null
        super.onDestroy()
    }
}
