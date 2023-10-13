package me.reezy.cosmo.pickerview.datetime

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import me.reezy.cosmo.pickerview.ComplexPickerView
import me.reezy.cosmo.pickerview.NumberFormatter
import me.reezy.cosmo.pickerview.PickerView
import java.util.*

class DatePickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ComplexPickerView(context, attrs, defStyleAttr) {

    private val pvYear = PickerView(context).init(attrs, isLoop = false)
    private val pvMonth = PickerView(context, attrs)
    private val pvDay = PickerView(context, attrs)

    private var onDateChangedListener: ((year: Int, month: Int, day: Int) -> Unit)? = null


    private val mTempDate = Calendar.getInstance()
    private val mMinDate = Calendar.getInstance().apply {
        set(1900, 0, 1)
    }
    private val mMaxDate = Calendar.getInstance().apply {
        set(2099, 11, 31)
    }

    private val mCurrentDate = Calendar.getInstance()


    init {
        addView(pvYear)
        addView(pvMonth)
        addView(pvDay)

        pvYear.setOnValueChangedListener {
            mTempDate.timeInMillis = mCurrentDate.timeInMillis
            mTempDate[Calendar.YEAR] = pvYear.selectedValue
            setDate(mTempDate[Calendar.YEAR], mTempDate[Calendar.MONTH], mTempDate[Calendar.DAY_OF_MONTH])
            updatePickers()
        }
        pvMonth.setOnValueChangedListener {
            mTempDate.timeInMillis = mCurrentDate.timeInMillis
            mTempDate[Calendar.MONTH] = pvMonth.selectedValue
            setDate(mTempDate[Calendar.YEAR], mTempDate[Calendar.MONTH], mTempDate[Calendar.DAY_OF_MONTH])
            updatePickers()
        }
        pvDay.setOnValueChangedListener {
            mTempDate.timeInMillis = mCurrentDate.timeInMillis
            mTempDate[Calendar.DAY_OF_MONTH] = pvDay.selectedValue
            setDate(mTempDate[Calendar.YEAR], mTempDate[Calendar.MONTH], mTempDate[Calendar.DAY_OF_MONTH])
            updatePickers()
        } 

        pvMonth.setFormatter { if (it < 9) "0${it + 1}" else (it + 1).toString() }
        pvDay.setFormatter(NumberFormatter.TwoDigit())

        updatePickers()
    }


    val year: Int get() = mCurrentDate[Calendar.YEAR]
    val month: Int get() = mCurrentDate[Calendar.MONTH]
    val day: Int get() = mCurrentDate[Calendar.DAY_OF_MONTH]


    var minDate: Long
        get() = mMinDate.timeInMillis
        set(value) {
            mTempDate.timeInMillis = value
            if (mTempDate[Calendar.YEAR] == mMinDate[Calendar.YEAR] && mTempDate[Calendar.DAY_OF_YEAR] == mMinDate[Calendar.DAY_OF_YEAR]) {
                return
            }
            mMinDate.timeInMillis = value

            if (mCurrentDate.before(mMinDate)) {
                mCurrentDate.timeInMillis = mMinDate.timeInMillis
                updatePickers()
            }
        }
    var maxDate: Long
        get() = mMaxDate.timeInMillis
        set(value) {
            mTempDate.timeInMillis = value
            if (mTempDate[Calendar.YEAR] == mMaxDate[Calendar.YEAR] && mTempDate[Calendar.DAY_OF_YEAR] == mMaxDate[Calendar.DAY_OF_YEAR]) {
                return
            }
            mMaxDate.timeInMillis = value
            if (mCurrentDate.after(mMaxDate)) {
                mCurrentDate.timeInMillis = mMaxDate.timeInMillis
                updatePickers()
            }
        }

    fun setMinDate(year: Int, month: Int, day: Int) {
        mTempDate.set(year, month, day)
        minDate = mTempDate.timeInMillis
    }
    fun setMaxDate(year: Int, month: Int, day: Int) {
        mTempDate.set(year, month, day)
        maxDate = mTempDate.timeInMillis
    }


    fun updateDate(year: Int, month: Int, day: Int) {

        if (!isNewDate(year, month, day)) {
            return
        }

        setDate(year, month, day)
        updatePickers()
    }

    fun setYearFormatter(formatter: NumberFormatter) {
        pvYear.setFormatter(formatter)
    }

    fun setMonthFormatter(formatter: NumberFormatter) {
        pvMonth.setFormatter(formatter)
    }

    fun setDayFormatter(formatter: NumberFormatter) {
        pvDay.setFormatter(formatter)
    }

    private fun isNewDate(year: Int, month: Int, day: Int): Boolean {
        return mCurrentDate[Calendar.YEAR] != year || mCurrentDate[Calendar.MONTH] != month || mCurrentDate[Calendar.DAY_OF_MONTH] != day
    }

    private fun setDate(year: Int, month: Int, day: Int) {
        mCurrentDate.set(year, month, day)
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.timeInMillis = mMinDate.timeInMillis
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.timeInMillis = mMaxDate.timeInMillis
        }
    }

    private fun updatePickers() {

        val minYear = mMinDate[Calendar.YEAR]
        val maxYear = mMaxDate[Calendar.YEAR]

        val minMonth = mMinDate[Calendar.MONTH]
        val maxMonth = mMaxDate[Calendar.MONTH]

        val currentYear = mCurrentDate[Calendar.YEAR]
        val currentMonth = mCurrentDate[Calendar.MONTH]

        pvYear.setItems(mMinDate[Calendar.YEAR], mMaxDate[Calendar.YEAR])

        when {
            minYear == maxYear -> pvMonth.setItems(minMonth, maxMonth)
            currentYear == minYear -> pvMonth.setItems(minMonth, 11)
            currentYear == maxYear -> pvMonth.setItems(0, maxMonth)
            else -> pvMonth.setItems(0, 11)
        }

        when {
            minYear == maxYear && minMonth == maxMonth -> pvDay.setItems(mMinDate[Calendar.DAY_OF_MONTH], mMaxDate[Calendar.DAY_OF_MONTH])
            currentYear == minYear && currentMonth == minMonth -> pvDay.setItems(mMinDate.get(Calendar.DAY_OF_MONTH), mMinDate.getActualMaximum(Calendar.DAY_OF_MONTH))
            currentYear == maxYear && currentMonth == maxMonth -> pvDay.setItems(1, mMaxDate.get(Calendar.DAY_OF_MONTH))
            else -> pvDay.setItems(1, mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        post {
            pvYear.selectedValue = mCurrentDate[Calendar.YEAR]
            pvMonth.selectedValue = mCurrentDate[Calendar.MONTH]
            pvDay.selectedValue = mCurrentDate[Calendar.DAY_OF_MONTH]

            onDateChangedListener?.invoke(mCurrentDate[Calendar.YEAR], mCurrentDate[Calendar.MONTH], mCurrentDate[Calendar.DAY_OF_MONTH])
        }

    }


    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { weight = 1f }


    fun setOnDateChangedListener(listener: (year: Int, month: Int, day: Int) -> Unit) {
        onDateChangedListener = listener
    }
}