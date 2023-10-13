package me.reezy.cosmo.pickerview.internal

import android.graphics.PointF
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class PickerLayoutManager(
    val visibleCount: Int = 3,
    val isLoop: Boolean = false
) : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {


    companion object {
        private const val DIRECTION_START = -1
        private const val DIRECTION_END = 1
    }

    private val offsetCount: Int = visibleCount / 2

    private val snapHelper = LinearSnapHelper()

    private val orientationHelper = OrientationHelper.createVerticalHelper(this)


    private var itemHeight: Int = 0

    private var scrollTargetPosition: Int = RecyclerView.NO_POSITION

    private var onTransformChildListener: ((view: TextView, offset: Int) -> Unit)? = null

    private var onSelectListener: ((position: Int) -> Unit)? = null

    init {
        if (visibleCount < 3 || visibleCount % 2 == 0) {
            throw IllegalArgumentException("visibleCount[$visibleCount] must be odd and >= 3")
        }
    }

    fun getCenterPosition(): Int {
        if (childCount == 0) return RecyclerView.NO_POSITION
        val centerView = getCenterView() ?: return RecyclerView.NO_POSITION
        return getPosition(centerView)
    }

    fun setOnTransformChildListener(listener: (view: TextView, offset: Int) -> Unit) {
        onTransformChildListener = listener
    }

    fun setOnSelectListener(listener: (position: Int) -> Unit) {
        onSelectListener = listener
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, itemHeight)
    }

    override fun isAutoMeasureEnabled(): Boolean = false

    override fun onMeasure(recycler: RecyclerView.Recycler, state: RecyclerView.State, widthSpec: Int, heightSpec: Int) {
        if (state.itemCount == 0) {
            super.onMeasure(recycler, state, widthSpec, heightSpec)
            return
        }
        if (state.isPreLayout) return

        val width = View.MeasureSpec.getSize(widthSpec)
        val height = View.MeasureSpec.getSize(heightSpec)

        itemHeight = height / visibleCount

        //设置宽高
        setMeasuredDimension(width, itemHeight * visibleCount)
    }

    // 软键盘的弹出和收起，scrollToPosition
    // 都会再次调用这个方法，自己要记录好偏移量
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (scrollTargetPosition != RecyclerView.NO_POSITION) {
            if (state.itemCount == 0) {
                removeAndRecycleAllViews(recycler)
                return
            }
        }
        if (state.isPreLayout) return

        val isScrollTo = scrollTargetPosition != RecyclerView.NO_POSITION

        val centerPosition = min(
            when {
                isScrollTo -> scrollTargetPosition
                childCount > 0 -> getCenterPosition()
                else -> 0
            }, state.itemCount - 1
        )
//        Logger.error("centerPosition == $centerPosition, scrollTargetPosition = $scrollTargetPosition, itemCount = ${state.itemCount}")

        // 先清空
        detachAndScrapAttachedViews(recycler)

        // 添加 item
        layoutChildren(recycler, centerPosition)

        // 处理缩放和透明度
        transformChildren()

        if (isScrollTo && centerPosition != scrollTargetPosition) {
            onSelectListener?.invoke(centerPosition)
        }
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        scrollTargetPosition = RecyclerView.NO_POSITION
    }

    override fun canScrollHorizontally(): Boolean = false

    override fun canScrollVertically(): Boolean = true

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int = 0

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (childCount == 0 || dy == 0) return 0

        val consume = handleScroll(dy, recycler, state)

        orientationHelper.offsetChildren(-consume)

        // 回收屏幕外的view
        recycleChildren(dy, recycler)

        // 处理缩放和透明度
        transformChildren()

        return consume
    }

    override fun scrollToPosition(position: Int) {
        super.scrollToPosition(position)
        if (childCount == 0) return

        require(position in 0 until itemCount) { "position[$position] must be >= 0 and < itemCount[$itemCount]" }


        scrollTargetPosition = position
        requestLayout()
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        if (childCount == 0) return

        require(position in 0 until itemCount) { "position[$position] must be >= 0 and < itemCount[$itemCount]" }

        val centerPosition = getCenterPosition()
        startSmoothScroll(LinearSmoothScroller(recyclerView.context).apply {
            targetPosition = if (centerPosition < position) position + offsetCount else position - offsetCount
        })
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) return null

        val centerPosition = getPosition(getCenterView()!!)
        val direction = if (targetPosition < centerPosition) -1 else 1
        return PointF(0f, direction.toFloat())
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (childCount == 0) return

        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            val centerView = getCenterView() ?: return
            val centerPosition = getPosition(centerView)

            val destination = (orientationHelper.totalSpace - itemHeight) / 2
            val distance = destination - orientationHelper.getDecoratedStart(centerView)

            if (distance != 0) {
                val rv = centerView.parent as RecyclerView
                rv.smoothScrollBy(0, -distance)
            } else {
                orientationHelper.offsetChildren(distance)
                onSelectListener?.invoke(centerPosition)
            }

        }
    }


    private fun layoutChildren(recycler: RecyclerView.Recycler, centerPosition: Int) {
        val start = if (isLoop) (centerPosition - offsetCount) else max(centerPosition - offsetCount, 0)
        val end = if (isLoop) (start + visibleCount) else min(start + visibleCount, itemCount)

        val startY = if (isLoop || centerPosition >= offsetCount) 0 else (offsetCount - centerPosition) * itemHeight


//        Logger.error("layoutChildren($centerPosition, $start, $end, $startY), $itemCount, $childCount")
        (start until end).forEach { position ->
            val child = recycler.getViewForPosition((position + itemCount) % itemCount)
            addView(child)

            measureChildWithMargins(child, 0, 0)

            val childY = startY + (position - start) * itemHeight
            layoutDecoratedWithMargins(child, paddingLeft, childY, width - paddingRight, childY + itemHeight)
        }
    }

    private fun handleScroll(delta: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {

        val direction = if (delta < 0) DIRECTION_START else DIRECTION_END

        val edge = getEdge(direction)

        var distance = abs(delta)

        // 检查是否滚动到了顶部或者底部
        if (checkScrollToEdge(direction, state)) {
            val offsetHeight = offsetCount * itemHeight
            return if (direction == DIRECTION_START) max(edge - offsetHeight, delta) else min(edge + offsetHeight, delta)
        }

        // 检查滚动距离是否可以填充下一个view
        if (distance < abs(edge)) {
//            Logger.error("abs($edge) > abs($delta)")
            return delta
        }

        while (distance > 0) {
            appendChild(recycler, state, direction)
            distance -= itemHeight
        }

        return delta
    }

    private fun getCenterView(): View? = snapHelper.findSnapView(this)

    private fun getEdge(direction: Int): Int = if (direction == DIRECTION_START) {
        orientationHelper.getDecoratedStart(getChildAt(0)) - orientationHelper.startAfterPadding
    } else {
        orientationHelper.getDecoratedEnd(getChildAt(childCount - 1)) - orientationHelper.endAfterPadding
    }

    private fun checkScrollToEdge(direction: Int, state: RecyclerView.State): Boolean = when {
        isLoop -> false
        direction == DIRECTION_START -> getPosition(getChildAt(0)!!) == 0
        direction == DIRECTION_END -> getPosition(getChildAt(childCount - 1)!!) == state.itemCount - 1
        else -> false
    }

    private fun appendChild(recycler: RecyclerView.Recycler, state: RecyclerView.State, direction: Int) {
        val itemCount = state.itemCount
        if (direction == DIRECTION_START) {
            val view = getChildAt(0)!!
            val position = getPosition(view)

            if (!isLoop && position <= 0) return

            val startY = orientationHelper.getDecoratedStart(view)
            val childPosition = (position + direction + itemCount) % itemCount
            val child = recycler.getViewForPosition(childPosition)

//            Logger.error("appendChild($direction, $startY, $position, $childPosition)")

            addView(child, 0)

            measureChildWithMargins(child, 0, 0)

            layoutDecoratedWithMargins(child, paddingLeft, startY - itemHeight, width - paddingRight, startY)
        } else {
            val view = getChildAt(childCount - 1)!!
            val position = getPosition(view)

            if (!isLoop && position >= itemCount - 1) return

            val startY = orientationHelper.getDecoratedEnd(view)
            val childPosition = (position + direction + itemCount) % itemCount
            val child = recycler.getViewForPosition(childPosition)

//            Logger.error("appendChild($direction, $startY, $position, $childPosition)")

            addView(child)

            measureChildWithMargins(child, 0, 0)

            layoutDecoratedWithMargins(child, paddingLeft, startY, width - paddingRight, startY + itemHeight)
        }
    }


    private val recycleViews: MutableList<View> = mutableListOf()

    private fun recycleChildren(delta: Int, recycler: RecyclerView.Recycler) {
        if (childCount <= visibleCount - offsetCount) return
        if (delta > 0) {
            val start = orientationHelper.startAfterPadding - itemHeight / 2
            for (i in 0 until childCount) {
                val child = getChildAt(i)!!
                if (orientationHelper.getDecoratedEnd(child) < start) {
                    recycleViews.add(child)
                } else {
                    break
                }
            }
        } else {
            val end = orientationHelper.endAfterPadding + itemHeight / 2
            for (i in (childCount - 1) downTo 0) {
                val child = getChildAt(i)!!
                if (orientationHelper.getDecoratedStart(child) > end) {
                    recycleViews.add(child)
                } else {
                    break
                }
            }
        }
        for (view in recycleViews) {
//            Logger.error("recycleChild(${getPosition(view)})")
            removeAndRecycleView(view, recycler)
        }
        recycleViews.clear()
    }

    private fun transformChildren() {
        if (childCount == 0) return

        val centerPosition = getPosition(getCenterView() ?: return)

        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            val position = getPosition(child)
            onTransformChildListener?.invoke(child as TextView, getOffset(centerPosition, position))
        }
    }

    private fun getOffset(centerPosition: Int, position: Int): Int {
        if (position == centerPosition) return 0
        val offset = position - centerPosition
        return when {
            !isLoop -> abs(offset)
            offset > offsetCount -> itemCount - position
            offset < -offsetCount -> position + 1
            else -> abs(offset)
        }
    }
}