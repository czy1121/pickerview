package me.reezy.cosmo.pickerview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import me.reezy.cosmo.R
import me.reezy.cosmo.pickerview.internal.NumberPickerAdapter
import me.reezy.cosmo.pickerview.internal.PickerItemDecoration
import me.reezy.cosmo.pickerview.internal.PickerLayoutManager

class PickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {

    private var onValueChangedListener: ((value: Int) -> Unit)? = null

    init {

        init(attrs)

        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
        adapter = NumberPickerAdapter()

    }

    fun init(attrs: AttributeSet? = null, isLoop: Boolean? = null): PickerView {

        val a = context.obtainStyledAttributes(attrs, R.styleable.PickerView)

        val pickerVisibleCount = a.getInt(R.styleable.PickerView_pickerVisibleCount, 3)
        val pickerIsLoop = isLoop ?: a.getBoolean(R.styleable.PickerView_pickerIsLoop, false)

        val sizeScale = a.getFloat(R.styleable.PickerView_pickerTextSizeScale, 1.0f)
        val alphaScale = a.getFloat(R.styleable.PickerView_pickerTextAlphaScale, 1.0f)

        val normalTextColor = a.getColor(R.styleable.PickerView_pickerNormalTextColor, Color.BLACK)
        val centerTextColor = a.getColor(R.styleable.PickerView_pickerCenterTextColor, Color.BLACK)

        val dividerHeight = a.getDimension(R.styleable.PickerView_pickerDividerHeight, 1.0f)
        val dividerColor = a.getColor(R.styleable.PickerView_pickerDividerColor, Color.LTGRAY)

        a.recycle()

        if (dividerHeight > 0) {
            this.addItemDecoration(PickerItemDecoration(dividerColor, dividerHeight))
        }

        layoutManager = PickerLayoutManager(pickerVisibleCount, pickerIsLoop)

        layoutManager.setOnSelectListener {
            onValueChangedListener?.invoke(it + adapter.minValue)
        }
        layoutManager.setOnTransformChildListener { view, offset ->
            view.setTextColor(if (offset == 0) centerTextColor else normalTextColor)
            view.setTypeface(null, if (offset == 0) Typeface.BOLD else Typeface.NORMAL)
            view.textSize = 16f * (1 - (1 - sizeScale) * offset)
            view.alpha = 1f - (1f - alphaScale) * offset
        }
        return this
    }

    override fun getAdapter(): NumberPickerAdapter = super.getAdapter() as NumberPickerAdapter

    override fun getLayoutManager(): PickerLayoutManager = super.getLayoutManager() as PickerLayoutManager

    var selectedPosition: Int
        get() = layoutManager.getCenterPosition()
        set(value) {
            layoutManager.scrollToPosition(value)
        }

    var selectedValue: Int
        get() = layoutManager.getCenterPosition() + adapter.minValue
        set(value) {
            layoutManager.scrollToPosition(value - adapter.minValue)
        }


    fun setItems(min: Int, max: Int) {
        adapter.setItems(min, max)
    }

    fun setItems(max: Int) {
        adapter.setItems(0, max)
    }

    fun setItems(items: List<String>) {
        adapter.setItems(items)
    }

    fun setOnValueChangedListener(listener: (position: Int) -> Unit) {
        onValueChangedListener = listener
    }

    fun setFormatter(formatter: NumberFormatter) {
        adapter.setFormatter(formatter)
    }
}