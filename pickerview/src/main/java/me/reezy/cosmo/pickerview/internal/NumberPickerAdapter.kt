package me.reezy.cosmo.pickerview.internal

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import me.reezy.cosmo.pickerview.NumberFormatter

class NumberPickerAdapter : RecyclerView.Adapter<TextViewHolder>() {

    private var numberFormatter: NumberFormatter? = null

    private var displayValues: List<String>? = null

    var minValue: Int = 0
        private set
    var maxValue: Int = 0
        private set


    @SuppressLint("NotifyDataSetChanged")
    fun setItems(min: Int, max: Int) {

        require(min >= 0) { "minValue must be >= 0" }
        require(max >= min) { "maxValue must be >= minValue" }

        minValue = min
        maxValue = max

        notifyDataSetChanged()
    }

    fun setItems(items: List<String>) {
        require(items.isNotEmpty()) { "displayValues must be not empty" }
        displayValues = items
        setItems(0, items.size - 1)
    }

    fun setFormatter(formatter: NumberFormatter) {
        numberFormatter = formatter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        return TextViewHolder(AppCompatTextView(parent.context) )
    }

    override fun getItemCount(): Int = maxValue - minValue + 1

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        holder.textView.text = displayValues?.get(position) ?: numberFormatter?.format(position + minValue) ?: (position + minValue).toString()
    }
}