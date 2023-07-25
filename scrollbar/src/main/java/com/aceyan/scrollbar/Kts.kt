package com.aceyan.scrollbar

import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

fun logE(msg: () -> String) {
    Log.e("ysw", msg())
}

fun RecyclerView.addFastScroller(
    mVerticalThumbDrawable: StateListDrawable,
    verticalTrackDrawable: Drawable,
    mHorizontalThumbDrawable: StateListDrawable,
    horizontalTrackDrawable: Drawable,
    defaultWidth: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
    scrollbarMinimumRange: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
    margin: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_margin)
) {
    generateFastScroller(
        mVerticalThumbDrawable,
        verticalTrackDrawable,
        mHorizontalThumbDrawable,
        horizontalTrackDrawable,
        defaultWidth,
        scrollbarMinimumRange, margin
    ).attachToRecyclerView(this)
}

fun RecyclerView.generateFastScroller(
    mVerticalThumbDrawable: StateListDrawable,
    verticalTrackDrawable: Drawable,
    mHorizontalThumbDrawable: StateListDrawable,
    horizontalTrackDrawable: Drawable,
    defaultWidth: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
    scrollbarMinimumRange: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
    margin: Int = resources.getDimensionPixelSize(R.dimen.fastscroll_margin)
): FastScroller {
    return FastScroller(
        this,
        mVerticalThumbDrawable,
        verticalTrackDrawable,
        mHorizontalThumbDrawable,
        horizontalTrackDrawable,
        defaultWidth,
        scrollbarMinimumRange,
        margin
    )
}