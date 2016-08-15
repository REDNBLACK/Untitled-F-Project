package org.f0w.fproject.server.utils;

import java.math.BigDecimal

fun String.containsAll(vararg strings: String): Boolean {
    for (string in strings) if (string !in this) return false

    return true
}

fun String?.toBigDecimalOrEmpty(): BigDecimal {
    return toBigDecimalOrDefault(0.0);
}

fun String?.toBigDecimalOrNull(): BigDecimal? {
    try {
        if (isNullOrEmpty()) throw NumberFormatException()

        return BigDecimal.valueOf(this!!.toDouble())
    } catch (e: NumberFormatException) {
        return null
    }
}

fun String?.toBigDecimalOrDefault(default: Double): BigDecimal {
    try {
        if (isNullOrEmpty()) throw NumberFormatException()

        return BigDecimal.valueOf(this!!.toDouble())
    } catch (e: NumberFormatException) {
        return BigDecimal.valueOf(default)
    }
}