package com.aceyan.scrollbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.text.StaticLayout
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.aceyan.scrollbar.FastScroller.AnimationState.Companion.ANIMATION_STATE_FADING_IN
import com.aceyan.scrollbar.FastScroller.AnimationState.Companion.ANIMATION_STATE_FADING_OUT
import com.aceyan.scrollbar.FastScroller.AnimationState.Companion.ANIMATION_STATE_IN
import com.aceyan.scrollbar.FastScroller.AnimationState.Companion.ANIMATION_STATE_OUT
import com.aceyan.scrollbar.FastScroller.DragState.Companion.DRAG_NONE
import com.aceyan.scrollbar.FastScroller.DragState.Companion.DRAG_X
import com.aceyan.scrollbar.FastScroller.DragState.Companion.DRAG_Y
import com.aceyan.scrollbar.FastScroller.State.Companion.STATE_DRAGGING
import com.aceyan.scrollbar.FastScroller.State.Companion.STATE_HIDDEN
import com.aceyan.scrollbar.FastScroller.State.Companion.STATE_VISIBLE

/**
 * Class responsible to animate and provide a fast scroller.
 */
open class FastScroller(
    recyclerView: RecyclerView, // Final values for the vertical scroll bar
    private val mVerticalThumbDrawable: StateListDrawable,
    private val verticalTrackDrawable: Drawable, // Final values for the horizontal scroll bar
    private val mHorizontalThumbDrawable: StateListDrawable,
    private val horizontalTrackDrawable: Drawable,
    private var defaultWidth: Int,
    private var scrollbarMinimumRange: Int,
    private var margin: Int
) : ItemDecoration(), OnItemTouchListener {
    companion object {
        private const val SHOW_DURATION_MS = 500
        private const val HIDE_DELAY_AFTER_VISIBLE_MS = 1500
        private const val HIDE_DELAY_AFTER_DRAGGING_MS = 1200
        private const val HIDE_DURATION_MS = 500
        private const val SCROLLBAR_FULL_OPAQUE = 255
    }

    private annotation class State {
        companion object {
            // Scroll thumb not showing
            const val STATE_HIDDEN = 0

            // Scroll thumb visible and moving along with the scrollbar
            const val STATE_VISIBLE = 1

            // Scroll thumb being dragged by user
            const val STATE_DRAGGING = 2
        }
    }

    private annotation class DragState {
        companion object {
            const val DRAG_NONE = 0
            const val DRAG_X = 1
            const val DRAG_Y = 2
        }
    }

    private annotation class AnimationState {
        companion object {
            const val ANIMATION_STATE_OUT = 0
            const val ANIMATION_STATE_FADING_IN = 1
            const val ANIMATION_STATE_IN = 2
            const val ANIMATION_STATE_FADING_OUT = 3
        }
    }

    private val PRESSED_STATE_SET = intArrayOf(android.R.attr.state_pressed)
    private val EMPTY_STATE_SET = intArrayOf()

    /**
     * Indicates the threshold condition for displaying the scroll bar
     */
    private val mScrollbarMinimumRange: Int
    private val mMargin: Int
    private val verticalThumbWidth: Int
    private val mVerticalTrackWidth: Int
    private val mHorizontalThumbHeight: Int
    private val mHorizontalTrackHeight: Int

    // Dynamic values for the vertical scroll bar
    private var verticalThumbHeight = 0
    private var verticalThumbCenterY = 0

    private var verticalDragY = 0f

    // Dynamic values for the horizontal scroll bar
    private var horizontalThumbWidth = 0
    private var horizontalThumbCenterX = 0

    private var horizontalDragX = 0f
    private var recyclerViewWidth = 0
    private var recyclerViewHeight = 0

    private var mRecyclerView: RecyclerView? = null

    /**
     * Whether the document is long/wide enough to require scrolling. If not, we don't show the
     * relevant scroller.
     */
    private var needVerticalScrollbar = false
    private var needHorizontalScrollbar = false

    @State
    private var mState = STATE_HIDDEN

    @DragState
    private var mDragState = DRAG_NONE

    private val mVerticalRange = IntArray(2)
    private val mHorizontalRange = IntArray(2)

    private val showHideAnimator = ValueAnimator.ofFloat(0f, 1f)

    @AnimationState
    private var animationState = ANIMATION_STATE_OUT

    private val mHideRunnable = Runnable { hide(HIDE_DURATION_MS) }

    private val isLayoutRTL: Boolean
        get() = ViewCompat.getLayoutDirection(mRecyclerView!!) == ViewCompat.LAYOUT_DIRECTION_RTL

    val isDragging: Boolean
        get() = mState == STATE_DRAGGING

    val isVisible: Boolean
        get() = mState == STATE_VISIBLE


    val horizontalThumbDrawable: Drawable
        get() = mHorizontalThumbDrawable

    val verticalThumbDrawable: Drawable
        get() = mVerticalThumbDrawable

    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar.
     */
    private val verticalRange: IntArray
        get() {
            mVerticalRange[0] = mMargin
            mVerticalRange[1] = recyclerViewHeight - mMargin
            return mVerticalRange
        }

    /**
     * Gets the (min, max) horizontal positions of the horizontal scroll bar.
     */
    private val horizontalRange: IntArray
        get() {
            mHorizontalRange[0] = mMargin
            mHorizontalRange[1] = recyclerViewWidth - mMargin
            return mHorizontalRange
        }

    private val mOnScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollPosition(
                    recyclerView.computeHorizontalScrollOffset(),
                    recyclerView.computeVerticalScrollOffset()
                )
            }
        }

    init {
        verticalThumbWidth = Math.max(defaultWidth, mVerticalThumbDrawable.intrinsicWidth)
        mVerticalTrackWidth = Math.max(defaultWidth, verticalTrackDrawable.intrinsicWidth)
        mHorizontalThumbHeight = Math
            .max(defaultWidth, mHorizontalThumbDrawable.intrinsicWidth)
        mHorizontalTrackHeight = Math
            .max(defaultWidth, horizontalTrackDrawable.intrinsicWidth)
        mScrollbarMinimumRange = scrollbarMinimumRange
        mMargin = margin
        mVerticalThumbDrawable.alpha = SCROLLBAR_FULL_OPAQUE
        verticalTrackDrawable.alpha = SCROLLBAR_FULL_OPAQUE
        showHideAnimator.addListener(AnimatorListener())
        showHideAnimator.addUpdateListener(AnimatorUpdater())
        attachToRecyclerView(recyclerView)
    }

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks()
        }
        mRecyclerView = recyclerView
        if (mRecyclerView != null) {
            setupCallbacks()
        }
    }

    private fun setupCallbacks() {
        mRecyclerView!!.addItemDecoration(this)
        mRecyclerView!!.addOnItemTouchListener(this)
        mRecyclerView!!.addOnScrollListener(mOnScrollListener)
    }

    private fun destroyCallbacks() {
        mRecyclerView!!.removeItemDecoration(this)
        mRecyclerView!!.removeOnItemTouchListener(this)
        mRecyclerView!!.removeOnScrollListener(mOnScrollListener)
        cancelHide()
    }

    fun requestRedraw() {
        mRecyclerView!!.invalidate()
    }

    fun setState(@State state: Int) {
        if (state == STATE_DRAGGING && mState != STATE_DRAGGING) {
            mVerticalThumbDrawable.state = PRESSED_STATE_SET
            cancelHide()
        }
        if (state == STATE_HIDDEN) {
            requestRedraw()
        } else {
            show()
        }
        if (mState == STATE_DRAGGING && state != STATE_DRAGGING) {
            mVerticalThumbDrawable.state = EMPTY_STATE_SET
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS)
        } else if (state == STATE_VISIBLE) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS)
        }
        mState = state
    }

    fun show() {
        when (animationState) {
            ANIMATION_STATE_FADING_OUT -> {
                showHideAnimator.cancel()
                animationState = ANIMATION_STATE_FADING_IN
                showHideAnimator.setFloatValues(showHideAnimator.animatedValue as Float, 1f)
                showHideAnimator.duration = SHOW_DURATION_MS.toLong()
                showHideAnimator.startDelay = 0
                showHideAnimator.start()
            }

            ANIMATION_STATE_OUT -> {
                animationState = ANIMATION_STATE_FADING_IN
                showHideAnimator.setFloatValues(showHideAnimator.animatedValue as Float, 1f)
                showHideAnimator.duration = SHOW_DURATION_MS.toLong()
                showHideAnimator.startDelay = 0
                showHideAnimator.start()
            }
        }
    }

    fun hide(duration: Int) {
        when (animationState) {
            ANIMATION_STATE_FADING_IN -> {
                showHideAnimator.cancel()
                animationState = ANIMATION_STATE_FADING_OUT
                showHideAnimator.setFloatValues(showHideAnimator.animatedValue as Float, 0f)
                showHideAnimator.duration = duration.toLong()
                showHideAnimator.start()
            }

            ANIMATION_STATE_IN -> {
                animationState = ANIMATION_STATE_FADING_OUT
                showHideAnimator.setFloatValues(showHideAnimator.animatedValue as Float, 0f)
                showHideAnimator.duration = duration.toLong()
                showHideAnimator.start()
            }
        }
    }

    private fun cancelHide() {
        mRecyclerView!!.removeCallbacks(mHideRunnable)
    }

    private fun resetHideDelay(delay: Int) {
        cancelHide()
        mRecyclerView!!.postDelayed(mHideRunnable, delay.toLong())
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (recyclerViewWidth != mRecyclerView!!.width
            || recyclerViewHeight != mRecyclerView!!.height
        ) {
            recyclerViewWidth = mRecyclerView!!.width
            recyclerViewHeight = mRecyclerView!!.height
            // This is due to the different events ordering when keyboard is opened or
            // retracted vs rotate. Hence to avoid corner cases we just disable the
            // scroller when size changed, and wait until the scroll position is recomputed
            // before showing it back.
            setState(STATE_HIDDEN)
            return
        }
        if (animationState != ANIMATION_STATE_OUT) {
            if (needVerticalScrollbar) {
                drawVerticalScrollbar(canvas)
            }
            if (needHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas)
            }
        }
    }

    private fun drawVerticalScrollbar(canvas: Canvas) {
        val left = recyclerViewWidth - verticalThumbWidth
        val top = verticalThumbCenterY - verticalThumbHeight / 2
        mVerticalThumbDrawable.setBounds(0, 0, verticalThumbWidth, verticalThumbHeight)
        verticalTrackDrawable.setBounds(0, 0, mVerticalTrackWidth, recyclerViewHeight)
        if (isLayoutRTL) {
            verticalTrackDrawable.draw(canvas)
            canvas.translate(verticalThumbWidth.toFloat(), top.toFloat())
            canvas.scale(-1f, 1f)
            mVerticalThumbDrawable.draw(canvas)
            canvas.scale(1f, 1f)
            canvas.translate(-verticalThumbWidth.toFloat(), -top.toFloat())
        } else {

            canvas.drawText("$verticalThumbCenterY", left - 100f, verticalThumbCenterY.toFloat(), Paint().apply { color = Color.RED } )

            canvas.translate(left.toFloat(), 0f)
            verticalTrackDrawable.draw(canvas)
            canvas.translate(0f, top.toFloat())
            mVerticalThumbDrawable.draw(canvas)
            canvas.translate(-left.toFloat(), -top.toFloat())
        }
    }

    private fun drawHorizontalScrollbar(canvas: Canvas) {
        val viewHeight = recyclerViewHeight
        val top = viewHeight - mHorizontalThumbHeight
        val left = horizontalThumbCenterX - horizontalThumbWidth / 2
        mHorizontalThumbDrawable.setBounds(0, 0, horizontalThumbWidth, mHorizontalThumbHeight)
        horizontalTrackDrawable
            .setBounds(0, 0, recyclerViewWidth, mHorizontalTrackHeight)
        canvas.translate(0f, top.toFloat())
        horizontalTrackDrawable.draw(canvas)
        canvas.translate(left.toFloat(), 0f)
        mHorizontalThumbDrawable.draw(canvas)
        canvas.translate(-left.toFloat(), -top.toFloat())
    }

    /**
     * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
     * the view itself.
     *
     * @param offsetX The new scroll X offset.
     * @param offsetY The new scroll Y offset.
     */
    fun updateScrollPosition(offsetX: Int, offsetY: Int) {

        logE { "updateScrollPosition(): offsetX = $offsetX, offsetY = $offsetY" }

        // Fort vertical
        // First to get the RV full items length (if a item height is 184px, item counts is 50, so the full length is 184px * 50 = 9200px)
        val verticalContentLength = mRecyclerView!!.computeVerticalScrollRange()
        val verticalVisibleLength = recyclerViewHeight
        //logE { "verticalContentLength = $verticalContentLength, v = $recyclerViewHeight" }
        needVerticalScrollbar = (verticalContentLength - verticalVisibleLength > 0
                && recyclerViewHeight >= mScrollbarMinimumRange)

        // For horizontal
        val horizontalContentLength = mRecyclerView!!.computeHorizontalScrollRange()
        val horizontalVisibleLength = recyclerViewWidth
        needHorizontalScrollbar = (horizontalContentLength - horizontalVisibleLength > 0
                && recyclerViewWidth >= mScrollbarMinimumRange)

        if (!needVerticalScrollbar && !needHorizontalScrollbar) {
            if (mState != STATE_HIDDEN) {
                setState(STATE_HIDDEN)
            }
            return
        }

        if (needVerticalScrollbar) {
            val middleScreenPos = offsetY + verticalVisibleLength / 2.0f
            verticalThumbCenterY =
                (verticalVisibleLength * middleScreenPos / verticalContentLength).toInt()

            logE { "verticalVisibleLength * middleScreenPos = ${verticalVisibleLength * middleScreenPos / verticalContentLength}, verticalThumbCenterY = $verticalThumbCenterY, rvH = $recyclerViewHeight" }

            verticalThumbHeight =
                verticalVisibleLength.coerceAtMost(verticalVisibleLength * verticalVisibleLength / verticalContentLength)
        }

        if (needHorizontalScrollbar) {
            val middleScreenPos = offsetX + horizontalVisibleLength / 2.0f
            horizontalThumbCenterX =
                (horizontalVisibleLength * middleScreenPos / horizontalContentLength).toInt()
            horizontalThumbWidth =
                horizontalVisibleLength.coerceAtMost(horizontalVisibleLength * horizontalVisibleLength / horizontalContentLength)
        }

        if (mState == STATE_HIDDEN || mState == STATE_VISIBLE) {
            setState(STATE_VISIBLE)
        }
    }

    override fun onInterceptTouchEvent(
        recyclerView: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        val handled: Boolean
        if (mState == STATE_VISIBLE) {
            val insideVerticalThumb = isPointInsideVerticalThumb(ev.x, ev.y)
            val insideHorizontalThumb = isPointInsideHorizontalThumb(ev.x, ev.y)
            if (ev.action == MotionEvent.ACTION_DOWN && (insideVerticalThumb || insideHorizontalThumb)) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    horizontalDragX = ev.x.toInt().toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    verticalDragY = ev.y.toInt().toFloat()
                }
                setState(STATE_DRAGGING)
                handled = true
            } else {
                handled = false
            }
        } else handled = mState == STATE_DRAGGING
        return handled
    }

    override fun onTouchEvent(recyclerView: RecyclerView, me: MotionEvent) {
        if (mState == STATE_HIDDEN) {
            return
        }
        if (me.action == MotionEvent.ACTION_DOWN) {
            val insideVerticalThumb = isPointInsideVerticalThumb(me.x, me.y)
            val insideHorizontalThumb = isPointInsideHorizontalThumb(me.x, me.y)
            if (insideVerticalThumb || insideHorizontalThumb) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    horizontalDragX = me.x.toInt().toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    verticalDragY = me.y.toInt().toFloat()
                }
                setState(STATE_DRAGGING)
            }
        } else if (me.action == MotionEvent.ACTION_UP && mState == STATE_DRAGGING) {
            verticalDragY = 0f
            horizontalDragX = 0f
            setState(STATE_VISIBLE)
            mDragState = DRAG_NONE
        } else if (me.action == MotionEvent.ACTION_MOVE && mState == STATE_DRAGGING) {
            show()
            if (mDragState == DRAG_X) {
                horizontalScrollTo(me.x)
            }
            if (mDragState == DRAG_Y) {
                verticalScrollTo(me.y)
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // nothing~
    }

    private fun verticalScrollTo(y: Float) {
        var tempY = y
        val scrollbarRange = verticalRange
        tempY = Math.max(scrollbarRange[0].toFloat(), Math.min(scrollbarRange[1].toFloat(), tempY))
        if (Math.abs(verticalThumbCenterY - tempY) < 2) {
            return
        }
        val scrollingBy = scrollTo(
            verticalDragY, tempY, scrollbarRange,
            mRecyclerView!!.computeVerticalScrollRange(),
            mRecyclerView!!.computeVerticalScrollOffset(), recyclerViewHeight
        )
        if (scrollingBy != 0) {
            mRecyclerView!!.scrollBy(0, scrollingBy)
        }
        verticalDragY = tempY
    }

    private fun horizontalScrollTo(x: Float) {
        var tempX = x
        val scrollbarRange = horizontalRange
        tempX = Math.max(scrollbarRange[0].toFloat(), Math.min(scrollbarRange[1].toFloat(), tempX))
        if (Math.abs(horizontalThumbCenterX - tempX) < 2) {
            return
        }
        val scrollingBy = scrollTo(
            horizontalDragX, tempX, scrollbarRange,
            mRecyclerView!!.computeHorizontalScrollRange(),
            mRecyclerView!!.computeHorizontalScrollOffset(), recyclerViewWidth
        )
        if (scrollingBy != 0) {
            mRecyclerView!!.scrollBy(scrollingBy, 0)
        }
        horizontalDragX = tempX
    }

    private fun scrollTo(
        oldDragPos: Float, newDragPos: Float, scrollbarRange: IntArray, scrollRange: Int,
        scrollOffset: Int, viewLength: Int
    ): Int {
        val scrollbarLength = scrollbarRange[1] - scrollbarRange[0]
        if (scrollbarLength == 0) {
            return 0
        }
        val percentage = (newDragPos - oldDragPos) / scrollbarLength.toFloat()
        val totalPossibleOffset = scrollRange - viewLength
        val scrollingBy = (percentage * totalPossibleOffset).toInt()
        val absoluteOffset = scrollOffset + scrollingBy
        return if (absoluteOffset < totalPossibleOffset && absoluteOffset >= 0) {
            scrollingBy
        } else {
            0
        }
    }

    private fun isPointInsideVerticalThumb(x: Float, y: Float): Boolean {
        return (if (isLayoutRTL) x <= verticalThumbWidth / 2 else x >= recyclerViewWidth - verticalThumbWidth)
                && y >= verticalThumbCenterY - verticalThumbHeight / 2
                && y <= verticalThumbCenterY + verticalThumbHeight / 2
    }

    private fun isPointInsideHorizontalThumb(x: Float, y: Float): Boolean {
        return y >= recyclerViewHeight - mHorizontalThumbHeight
                && x >= horizontalThumbCenterX - horizontalThumbWidth / 2
                && x <= horizontalThumbCenterX + horizontalThumbWidth / 2
    }

    private inner class AnimatorListener() : AnimatorListenerAdapter() {
        private var canceled = false

        override fun onAnimationEnd(animation: Animator) {
            // Cancel is always followed by a new directive, so don't update state.
            if (canceled) {
                canceled = false
                return
            }
            if (showHideAnimator.animatedValue as Float == 0f) {
                animationState = ANIMATION_STATE_OUT
                setState(STATE_HIDDEN)
            } else {
                animationState = ANIMATION_STATE_IN
                requestRedraw()
            }
        }

        override fun onAnimationCancel(animation: Animator) {
            canceled = true
        }
    }

    private inner class AnimatorUpdater() : AnimatorUpdateListener {
        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
            val alpha = (SCROLLBAR_FULL_OPAQUE * valueAnimator.animatedValue as Float).toInt()
            mVerticalThumbDrawable.alpha = alpha
            verticalTrackDrawable.alpha = alpha
            requestRedraw()
        }
    }
}