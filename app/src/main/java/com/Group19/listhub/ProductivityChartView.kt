package com.Group19.listhub

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class ProductivityChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var completed = 0
    private var pending = 0
    private var overdue = 0

    fun setStats(completed: Int, pending: Int, overdue: Int) {
        this.completed = completed
        this.pending = pending
        this.overdue = overdue
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val values = listOf(completed, pending, overdue)
        val labels = listOf("Completed", "Pending", "Overdue")
        val colors = listOf(0xFF35A77A.toInt(), 0xFF7183C5.toInt(), 0xFFE16B62.toInt())
        val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val left = paddingLeft.toFloat() + 8f
        val available = width - left - paddingRight - 12f
        val barHeight = 15f
        val gap = 16f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        labels.forEachIndexed { index, label ->
            val y = paddingTop + index * (barHeight + gap) + 14f
            paint.color = 0xFF344054.toInt()
            canvas.drawText("$label  ${values[index]}", left, y, paint)
            val top = y + 5f
            val widthBar = (available * values[index] / maxValue).coerceAtLeast(if (values[index] > 0) 8f else 0f)
            paint.color = 0xFFE9EAF0.toInt()
            canvas.drawRoundRect(left, top, left + available, top + barHeight, 8f, 8f, paint)
            paint.color = colors[index]
            canvas.drawRoundRect(left, top, left + widthBar, top + barHeight, 8f, 8f, paint)
        }
    }
}
