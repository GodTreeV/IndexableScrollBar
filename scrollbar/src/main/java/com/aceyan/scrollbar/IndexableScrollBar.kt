package com.aceyan.scrollbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IndexableScrollBar @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    companion object {
        private const val MAX_TRACK_ALPHA = 30
        private const val SCROLL_BAR_VIS_DURATION = 150L
    }

    private val trackPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
        }
    }

    private val thumbPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {

        }
    }

    private lateinit var recyclerView: RecyclerView
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var mDy = 0f

    private var onScrollListener:RecyclerView.OnScrollListener? = null

    private val trackRect = Rect()
    private val thumbRect = Rect()

    init {
        trackPaint.apply {
            alpha = MAX_TRACK_ALPHA
            color = Color.GRAY
            style = Paint.Style.FILL
        }

        thumbPaint.apply {
            color = Color.BLUE
            strokeCap = Paint.Cap.ROUND
        }

        thumbRect.set(0, 0, 20, 100)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        trackRect.set(0, 0, measuredWidth, measuredHeight)
        thumbRect.right = measuredWidth
        logE { "$trackRect" }
        canvas.drawRect(trackRect, trackPaint)
        canvas.save()
        canvas.translate(0f, mDy)
        canvas.drawRect(thumbRect, thumbPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateThumbPosition(0f, lastTouchY - event.y)
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP or MotionEvent.ACTION_CANCEL -> {
                lastTouchX = 0f
                lastTouchY = 0f
                isDragging = false
            }
        }
        return isDragging
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        if (onScrollListener != null) {
            this.recyclerView.removeOnScrollListener(onScrollListener!!)
        }
        onScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateThumbPosition(0f, recyclerView.computeVerticalScrollOffset().toFloat())
            }
        }.also {
            this.recyclerView.addOnScrollListener(it)
        }
    }

    fun updateThumbPosition(dx: Float, dy: Float) {
        val adapter = recyclerView.adapter!!
        val lm = recyclerView.layoutManager!!
        val ratio = (lm as LinearLayoutManager).findFirstVisibleItemPosition() / lm.itemCount.toFloat()
        mDy = dy.toFloat()

        invalidate()
    }
}