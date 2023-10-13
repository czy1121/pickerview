package me.reezy.cosmo.pickerview.datetime

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import me.reezy.cosmo.pickerview.ComplexPickerView
import me.reezy.cosmo.pickerview.NumberFormatter
import me.reezy.cosmo.pickerview.PickerView

class TimePickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ComplexPickerView(context, attrs, defStyleAttr) {

    private val pv1 = PickerView(context).init(attrs, isLoop = false)
    private val pv2 = PickerView(context, attrs)
    private val pv3 = PickerView(context, attrs)

    private var onTimeChangedListener: ((hour: Int, minute: Int) -> Unit)? = null

    private var hourFormatter: NumberFormatter = NumberFormatter.TwoDigit()

    var is24HourMode: Boolean = true
        set(value) {
            if (field == value) return
            field = value

            updatePickers()
            post {
                updateHour(false)
            }
        }

    init {
        addView(pv1)
        addView(pv2)
        addView(pv3)

        pv1.setOnValueChangedListener {
            hour = it * 12 + hour % 12
        }
        pv2.setOnValueChangedListener {
            hour = if (is24HourMode) it else (pv1.selectedPosition * 12 + it)
        }
        pv3.setOnValueChangedListener {
            minute = it
        }

        pv1.setFormatter { if (it == 0) "上午" else "下午" }
        pv2.setFormatter { hourFormatter.format(if (is24HourMode) it else ((it + 11) % 12 + 1)) }
        pv3.setFormatter(NumberFormatter.TwoDigit())

        pv1.setItems(1)
        pv3.setItems(59)
        updatePickers()
    }

    fun setTwelveHourFormatter(formatter: NumberFormatter) {
        pv1.setFormatter(formatter)
    }

    fun setHourFormatter(formatter: NumberFormatter) {
        hourFormatter = formatter
    }

    fun setMinuteFormatter(formatter: NumberFormatter) {
        pv3.setFormatter(formatter)
    }


    var hour: Int = 0
        set(value) {
            if (field == value) return
            field = value
            post {
                updateHour(true)
            }
        }
    var minute: Int = 0
        set(value) {
            if (field == value) return
            field = value
            post {
                pv3.selectedValue = value
                onTimeChangedListener?.invoke(hour, value)
            }
        }

    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { weight = 1f }


    fun setOnTimeChangedListener(listener: (hour: Int, minute: Int) -> Unit) {
        onTimeChangedListener = listener
    }

    private fun updateHour(dispatch: Boolean) {
        if (is24HourMode) {
            pv2.selectedValue = hour
        } else {
            pv1.selectedValue = hour / 12
            pv2.selectedValue = hour % 12
            if (dispatch) {
                onTimeChangedListener?.invoke(hour, minute)
            }
        }
    }

    private fun updatePickers() {
        if (is24HourMode) {
            pv1.visibility = View.GONE
            pv2.setItems(0, 23)
        } else {
            pv1.visibility = View.VISIBLE
            pv2.setItems(0, 11)
        }
    }
}