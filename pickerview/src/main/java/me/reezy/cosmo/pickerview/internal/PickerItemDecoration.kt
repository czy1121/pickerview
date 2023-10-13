package me.reezy.cosmo.pickerview.internal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView

class PickerItemDecoration(
    private val dividerColor: Int = Color.LTGRAY,
    private val dividerSize: Float = 1.0f,
) : RecyclerView.ItemDecoration() {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dividerColor
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)
        if (parent.layoutManager == null || parent.layoutManager !is PickerLayoutManager)
            return

        val lm = parent.layoutManager as PickerLayoutManager
        val itemSize = parent.height / lm.visibleCount

        val position = lm.visibleCount / 2
        val top = position * itemSize - dividerSize / 2
        val right = parent.width.toFloat()

        canvas.drawRect(0f, top, right, top + dividerSize, mPaint)
        canvas.drawRect(0f, top + itemSize, right, top + itemSize + dividerSize, mPaint)
    }
}