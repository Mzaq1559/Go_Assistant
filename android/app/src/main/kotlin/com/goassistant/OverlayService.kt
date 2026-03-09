package com.goassistant

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Base64
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OverlayService : Service() {

    companion object {
        var isRunning = false
        var projectionResultCode: Int = 0
        var projectionData: Intent? = null
        const val CHANNEL_ID = "GoAssistantOverlay"
    }

    // ── Config ──────────────────────────────────
    private var apiKey    = ""
    private var turn      = "black"
    private var boardSize = 19

    // ── Overlay views ────────────────────────────
    private lateinit var wm:         WindowManager
    private lateinit var bubbleView: View
    private lateinit var overlayCanvas: OverlayCanvasView

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var canvasParams: WindowManager.LayoutParams? = null

    // ── Screenshot ───────────────────────────────
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay:  VirtualDisplay?  = null
    private var imageReader:     ImageReader?     = null

    // ── State ────────────────────────────────────
    private var isAnalyzing = false
    private val scope       = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── Metrics ─────────────────────────────────
    private var screenW = 1080
    private var screenH = 1920
    private var density = 3.0f

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        getScreenMetrics()
        createNotificationChannel()
        startForeground(1, buildNotification())
        setupMediaProjection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        apiKey    = intent?.getStringExtra("apiKey")    ?: ""
        turn      = intent?.getStringExtra("turn")      ?: "black"
        boardSize = intent?.getIntExtra("boardSize", 19) ?: 19

        addBubble()
        addOverlayCanvas()
        return START_STICKY
    }

    private fun getScreenMetrics() {
        val dm = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            screenW = bounds.width()
            screenH = bounds.height()
            density = resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(dm)
            screenW = dm.widthPixels
            screenH = dm.heightPixels
            density = dm.density
        }
    }

    // ────────────────────────────────────────────
    //  Floating bubble button
    // ────────────────────────────────────────────

    private fun addBubble() {
        bubbleView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_bubble, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        bubbleParams = WindowManager.LayoutParams(
            dp(64), dp(64),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - dp(80)
            y = screenH / 3
        }

        wm.addView(bubbleView, bubbleParams)

        // Drag to reposition
        var initialX = 0; var initialY = 0
        var touchX   = 0f; var touchY = 0f
        var moved    = false

        bubbleView.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams!!.x; initialY = bubbleParams!!.y
                    touchX = ev.rawX; touchY = ev.rawY
                    moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - touchX).toInt()
                    val dy = (ev.rawY - touchY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                    bubbleParams!!.x = initialX + dx
                    bubbleParams!!.y = initialY + dy
                    wm.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onBubbleTapped()
                    true
                }
                else -> false
            }
        }

        updateBubbleState()
    }

    private fun onBubbleTapped() {
        if (isAnalyzing) return
        // Hide overlay canvas first so it doesn't appear in screenshot
        overlayCanvas.visibility = View.INVISIBLE
        bubbleView.visibility    = View.INVISIBLE

        // Small delay so views are hidden before screenshot
        Handler(Looper.getMainLooper()).postDelayed({
            takeScreenshotAndAnalyze()
        }, 200)
    }

    private fun updateBubbleState() {
        val btn = bubbleView.findViewById<TextView>(R.id.bubbleBtn)
        btn.text = when {
            isAnalyzing -> "⏳"
            turn == "black" -> "⚫"
            else -> "⚪"
        }
    }

    // ────────────────────────────────────────────
    //  Transparent canvas overlay (draws the circle)
    // ────────────────────────────────────────────

    private fun addOverlayCanvas() {
        overlayCanvas = OverlayCanvasView(this)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        wm.addView(overlayCanvas, canvasParams)
    }

    // ────────────────────────────────────────────
    //  Screenshot via MediaProjection
    // ────────────────────────────────────────────

    private fun setupMediaProjection() {
        val code = projectionResultCode
        val data = projectionData ?: return
        val mgr  = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(code, data)
    }

    private fun takeScreenshotAndAnalyze() {
        if (mediaProjection == null) {
            showError("Screenshot permission not granted")
            restoreVisibility()
            return
        }

        imageReader?.close()
        virtualDisplay?.release()

        imageReader = ImageReader.newInstance(screenW, screenH,
            PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "GoCapture", screenW, screenH, density.toInt(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                restoreVisibility()
                showError("Could not capture screen")
                return@postDelayed
            }

            val planes = image.planes
            val buffer = planes[0].buffer
            val rowPad = planes[0].rowStride - planes[0].pixelStride * screenW
            val bmp    = Bitmap.createBitmap(
                screenW + rowPad / planes[0].pixelStride,
                screenH, Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop to actual screen width
            val cropped = Bitmap.createBitmap(bmp, 0, 0, screenW, screenH)
            bmp.recycle()

            virtualDisplay?.release()
            imageReader?.close()

            restoreVisibility()
            sendToClaudeAndDraw(cropped)
        }, 300)
    }

    private fun restoreVisibility() {
        Handler(Looper.getMainLooper()).post {
            bubbleView.visibility    = View.VISIBLE
            overlayCanvas.visibility = View.VISIBLE
        }
    }

    // ────────────────────────────────────────────
    //  Claude API call
    // ────────────────────────────────────────────

    private fun sendToClaudeAndDraw(bmp: Bitmap) {
        isAnalyzing = true
        updateBubbleState()

        scope.launch {
            try {
                val b64 = withContext(Dispatchers.IO) { bitmapToBase64(bmp) }
                bmp.recycle()

                val result = withContext(Dispatchers.IO) {
                    callClaudeApi(b64)
                }

                // Draw on overlay
                overlayCanvas.setMove(
                    move      = result.move,
                    boardSize = result.boardSize,
                    screenW   = screenW,
                    screenH   = screenH,
                    colFrac   = result.colFraction,
                    rowFrac   = result.rowFraction,
                    winrate   = result.winrate,
                    scoreStr  = result.score,
                    reason    = result.reasoning,
                )

                turn = if (turn == "black") "white" else "black"

            } catch (e: Exception) {
                showError(e.message ?: "Error")
            } finally {
                isAnalyzing = false
                updateBubbleState()
            }
        }
    }

    private fun bitmapToBase64(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun callClaudeApi(b64: String): ClaudeResult {
        val sizeHint = if (boardSize > 0) "The board is ${boardSize}×${boardSize}." else ""
        val prompt = """
You are an expert Go AI. Analyze this Go board screenshot and suggest the best move for $turn.
$sizeHint

Respond ONLY with this JSON (no other text):
{
  "move": "D16",
  "board_size": 19,
  "col_fraction": 0.21,
  "row_fraction": 0.18,
  "winrate": 0.58,
  "score": "+2.5",
  "reasoning": "Brief reason"
}

Rules:
- "move": GTP notation e.g. "D4". Use "pass" only if truly no good move.
- "board_size": 9, 13, or 19 as you detect.
- "col_fraction": the x position of the move as a fraction of the BOARD WIDTH (0.0=left edge, 1.0=right edge).
- "row_fraction": the y position of the move as a fraction of the BOARD HEIGHT (0.0=top edge, 1.0=bottom edge).
- "winrate": $turn's win probability 0.0-1.0.
- "score": point lead like "+3.5" or "Even".
- "reasoning": 1 sentence why.

The col_fraction and row_fraction must precisely indicate where on the SCREEN the recommended intersection is located within the visible board area.
""".trimIndent()

        val body = JSONObject().apply {
            put("model", "claude-opus-4-5")
            put("max_tokens", 300)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", b64)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                    })
                })
            })
        }.toString()

        val url  = URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as HttpsURLConnection
        conn.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
            connectTimeout = 15000
            readTimeout    = 30000
        }
        conn.outputStream.write(body.toByteArray())

        val code = conn.responseCode
        val resp = if (code == 200)
            conn.inputStream.bufferedReader().readText()
        else
            conn.errorStream.bufferedReader().readText()

        conn.disconnect()

        if (code != 200) {
            val err = JSONObject(resp)
            throw Exception(err.optJSONObject("error")?.optString("message") ?: "API error $code")
        }

        val content = JSONObject(resp)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        // Extract JSON object
        val jsonStr = Regex("\\{[\\s\\S]*\\}").find(content)?.value
            ?: throw Exception("Could not parse response")

        val json = JSONObject(jsonStr)
        return ClaudeResult(
            move        = json.optString("move", "pass"),
            boardSize   = json.optInt("board_size", boardSize.takeIf { it > 0 } ?: 19),
            colFraction = json.optDouble("col_fraction", 0.5),
            rowFraction = json.optDouble("row_fraction", 0.5),
            winrate     = json.optDouble("winrate", 0.5),
            score       = json.optString("score", "Even"),
            reasoning   = json.optString("reasoning", ""),
        )
    }

    // ────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────

    private fun showError(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Go Assistant: $msg", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int) = (value * density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Go Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Floating Go analysis overlay" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply {
                action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Go Assistant")
            .setContentText("Overlay active – tap the bubble to analyze")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        runCatching { wm.removeView(bubbleView) }
        runCatching { wm.removeView(overlayCanvas) }
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?) = null
}

data class ClaudeResult(
    val move:        String,
    val boardSize:   Int,
    val colFraction: Double,
    val rowFraction: Double,
    val winrate:     Double,
    val score:       String,
    val reasoning:   String,
)
