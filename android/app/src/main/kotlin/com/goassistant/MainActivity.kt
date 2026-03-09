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

    private val CHANNEL = "com.goassistant/overlay"
    private val REQ_OVERLAY   = 1001
    private val REQ_PROJECTION = 1002

    private var projectionResult: MethodChannel.Result? = null
    private var overlayResult:    MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {

                    // Check if we can draw overlays
                    "hasOverlayPermission" -> {
                        result.success(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                Settings.canDrawOverlays(this)
                            else true
                        )
                    }

                    // Open system settings for overlay permission
                    "requestOverlayPermission" -> {
                        overlayResult = result
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivityForResult(intent, REQ_OVERLAY)
                        } else {
                            result.success(true)
                        }
                    }

                    // Ask for MediaProjection permission (screenshot)
                    "requestScreenCapture" -> {
                        projectionResult = result
                        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                as MediaProjectionManager
                        startActivityForResult(
                            mgr.createScreenCaptureIntent(), REQ_PROJECTION
                        )
                    }

                    // Start the floating overlay service
                    "startOverlay" -> {
                        val apiKey   = call.argument<String>("apiKey") ?: ""
                        val turn     = call.argument<String>("turn")   ?: "black"
                        val boardSize = call.argument<Int>("boardSize") ?: 19
                        val intent   = Intent(this, OverlayService::class.java).apply {
                            putExtra("apiKey",    apiKey)
                            putExtra("turn",      turn)
                            putExtra("boardSize", boardSize)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            startForegroundService(intent)
                        else
                            startService(intent)
                        result.success(true)
                    }

                    // Stop the floating overlay
                    "stopOverlay" -> {
                        stopService(Intent(this, OverlayService::class.java))
                        result.success(true)
                    }

                    // Check if overlay service is running
                    "isOverlayRunning" -> {
                        result.success(OverlayService.isRunning)
                    }

                    else -> result.notImplemented()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_OVERLAY -> {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    Settings.canDrawOverlays(this) else true
                overlayResult?.success(granted)
                overlayResult = null
            }
            REQ_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Pass the projection token to the service
                    OverlayService.projectionResultCode = resultCode
                    OverlayService.projectionData       = data
                    projectionResult?.success(true)
                } else {
                    projectionResult?.success(false)
                }
                projectionResult = null
            }
        }
    }
}
