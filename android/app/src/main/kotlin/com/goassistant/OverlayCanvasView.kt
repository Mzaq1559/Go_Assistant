package com.goassistant

import android.content.Context
import android.graphics.*
import android.view.View

class OverlayCanvasView(context: Context) : View(context) {
    private var moveLabel = ""
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var hasDraw = false
    private var reasoning = ""
    private var score = ""
    private var winPct = 0

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xFFD4A843.toInt(); strokeWidth = 5f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4A843.toInt() }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xCCD4A843.toInt(); strokeWidth = 2f
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD0D1117.toInt() }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4A843.toInt(); textSize = 38f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 26f; textAlign = Paint.Align.CENTER
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B949E.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER
    }

    fun setMove(
        move: String, boardSize: Int, screenW: Int, screenH: Int,
        colFrac: Double = 0.5, rowFrac: Double = 0.5,
        winrate: Double = 0.5, scoreStr: String = "", reason: String = ""
    ) {
        moveLabel = move.uppercase()
        val safeW = screenW.coerceAtLeast(1)
        val safeH = screenH.coerceAtLeast(1)
        val safeBoard = boardSize.coerceIn(9, 19)
        cx = (safeW * colFrac.coerceIn(0.0, 1.0)).toFloat()
        cy = (safeH * rowFrac.coerceIn(0.0, 1.0)).toFloat()
        radius = (safeW.toFloat() / safeBoard * 0.45f).coerceIn(18f, 52f)
        hasDraw = !move.equals("pass", true) && !move.equals("resign", true)
        reasoning = reason.trim()
        score = scoreStr.trim()
        winPct = (winrate.coerceIn(0.0, 1.0) * 100).toInt()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasDraw) return

        val glow = radius * 2.5f
        glowPaint.shader = RadialGradient(cx, cy, glow,
            intArrayOf(0x55D4A843.toInt(), 0x00D4A843.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, glow, glowPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawCircle(cx, cy, radius * 0.22f, dotPaint)

        val arm = radius * 1.8f
        canvas.drawLine(cx - arm, cy, cx + arm, cy, linePaint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, linePaint)

        val cardW = minOf(280f, (width - 32).toFloat().coerceAtLeast(1f))
        val cardH = 110f
        var cardX = cx - cardW / 2f
        var cardY = cy + radius + 16f
        if (cardY + cardH > height) cardY = cy - radius - cardH - 16f
        cardX = cardX.coerceIn(16f, (width - cardW - 16f).coerceAtLeast(16f))

        canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardW, cardY + cardH), 18f, 18f, labelBgPaint)
        canvas.drawText(moveLabel, cardX + cardW / 2f, cardY + 48f, labelPaint)

        val info = buildString {
            if (score.isNotEmpty()) append(score).append("  ")
            append("Win ").append(winPct).append("%")
        }
        canvas.drawText(info, cardX + cardW / 2f, cardY + 78f, subPaint)

        if (reasoning.isNotEmpty()) {
            val short = if (reasoning.length > 38) reasoning.take(35) + "…" else reasoning
            canvas.drawText(short, cardX + cardW / 2f, cardY + 103f, mutedPaint)
        }
    }
}
