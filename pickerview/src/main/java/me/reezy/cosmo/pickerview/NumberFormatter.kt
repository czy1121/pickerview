package me.reezy.cosmo.pickerview

fun interface NumberFormatter {
    fun format(number: Int): String

    class TwoDigit: NumberFormatter {
        override fun format(number: Int): String {
            return if (number < 10) "0$number" else number.toString()
        }
    }
}