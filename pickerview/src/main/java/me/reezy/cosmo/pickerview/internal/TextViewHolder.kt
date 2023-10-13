package me.reezy.cosmo.pickerview.internal

import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TextViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
    init {
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.gravity = Gravity.CENTER
        textView.maxLines = 1
        textView.textSize = 15f
    }
}