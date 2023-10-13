package me.reezy.cosmo.pickerview.chinesedate

import android.content.Context
import android.util.AttributeSet
import me.reezy.cosmo.chinesedate.LunarDate
import me.reezy.cosmo.chinesedate.util.LunarDateUtil
import me.reezy.cosmo.pickerview.ComplexPickerView
import me.reezy.cosmo.pickerview.NumberFormatter
import me.reezy.cosmo.pickerview.PickerView
import java.util.*

class ChineseDatePickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ComplexPickerView(context, attrs, defStyleAttr) {

    /** * month 从0开始 */
    private data class Ymd(val year: Int, val month: Int, val day: Int) {
        val date: Int get() = year * 10000 + month * 100 + day
    }

    private var onDateChangedListener: ((year: Int, month: Int, day: Int) -> Unit)? = null

    private val pvYear = PickerView(context).init(attrs)
    private val pvMonth = PickerView(context, attrs)
    private val pvDay = PickerView(context, attrs)

    private val gMinDate = Ymd(1901, 0, 1)      // => chinese Ymd(1900, 10, 11)
    private val gMaxDate = Ymd(2100, 11, 31)    // => chinese Ymd(2100, 11, 1)

    private val cMinDate = Ymd(1900, 10, 11)   // => gregorian Ymd(1901, 0, 1)
    private val cMaxDate = Ymd(2100, 11, 1)    // => gregorian Ymd(2100, 11, 31)


    private var gregorian = Ymd(2000, 0, 1)
    private var display = Ymd(gregorian.year, gregorian.month, gregorian.day)

    private val calendar = Calendar.getInstance()


    val year: Int get() = gregorian.year
    val month: Int get() = gregorian.month
    val day: Int get() = gregorian.day

    init {
        addView(pvYear)
        addView(pvMonth)
        addView(pvDay)

        pvYear.setOnValueChangedListener {
            setDate(display.copy(year = it))
        }
        pvMonth.setOnValueChangedListener {
            setDate(display.copy(month = it))
        }
        pvDay.setOnValueChangedListener {
            setDate(display.copy(day = it))
        }

        pvYear.setFormatter { "${it}年" }
        pvMonth.setFormatter { if (isChineseDateMode) LunarDate.month(display.year, it) else "${it + 1}月" }
        pvDay.setFormatter { if (isChineseDateMode) LunarDate.day(it) else "${it}日" }

        setDate(Ymd(calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH]))
    }


    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply { weight = 1f }

    fun setOnDateChangedListener(listener: (year: Int, month: Int, day: Int) -> Unit) {
        onDateChangedListener = listener
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

    var isChineseDateMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateMode()
        }

    fun updateDate(year: Int, month: Int, day: Int) {
        val ymd = Ymd(year, month, day)

        if (display == ymd) {
            return
        }

        if (isChineseDateMode) {
            require(year in cMinDate.year..cMaxDate.year) { "year must be in range(${cMinDate.year}, ${cMaxDate.year}) " }

            val maxMonth = LunarDateUtil.getMonthCount(year) - 1
            require(month in 0..maxMonth) { "month of $year must be in range(0, $maxMonth) " }

            val maxDay = LunarDateUtil.getMonthDays(year, month)
            require(day in 1..maxDay) { "day of $year-$month must be in range(1, $maxDay) " }
        } else {
            calendar.set(ymd.year, ymd.month, ymd.day)
        }

        setDate(ymd)
    }

    private fun updateMode() {
        if (isChineseDateMode) {
            val (y, m, d, l) = LunarDateUtil.fromSolarDate(display.year, display.month + 1, display.day)
            val monthIndex = LunarDateUtil.getMonthIndex(y, m, l == 1)
            setDate(Ymd(y, monthIndex, d))
        } else {
            val (y, m, d) = LunarDateUtil.toSolarDate(display.year, display.month, display.day)
            setDate(Ymd(1901 + (y - 1901 + 200) % 200, m - 1, d))
        }
    }

    private fun setDate(ymd: Ymd) {
        val minDate = if (isChineseDateMode) cMinDate else gMinDate
        val maxDate = if (isChineseDateMode) cMaxDate else gMaxDate

        val date = ymd.date

        display = when {
            date < minDate.date -> minDate
            date > maxDate.date -> maxDate
            else -> ymd
        }

        pvYear.setItems(minDate.year, maxDate.year)

        if (isChineseDateMode) {
            val (y, m, d) = LunarDateUtil.toSolarDate(display.year, display.month, display.day)
            gregorian = Ymd(y, m - 1, d)

            pvMonth.setItems(0, LunarDateUtil.getMonthCount(display.year) - 1)
            pvDay.setItems(1, LunarDateUtil.getMonthDays(display.year, display.month))
        } else {
            gregorian = display

            calendar.set(display.year, display.month, display.day)
            pvMonth.setItems(0, 11)
            pvDay.setItems(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        post {
            pvYear.selectedValue = display.year
            pvMonth.selectedValue = display.month
            pvDay.selectedValue = display.day

            onDateChangedListener?.invoke(gregorian.year, gregorian.month, gregorian.day)
        }

    }

}