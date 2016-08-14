package org.f0w.fproject.server.utils;

import java.math.BigDecimal

fun String.toBigDecimalOrEmpty(): BigDecimal {
    return toBigDecimalOrDefault(0.0);
}

fun String.toBigDecimalOrNull(): BigDecimal? {
    try {
        return BigDecimal.valueOf(toDouble())
    } catch (e: NumberFormatException) {
        return null
    }
}

fun String.toBigDecimalOrDefault(default: Double): BigDecimal {
    try {
        return BigDecimal.valueOf(toDouble())
    } catch (e: NumberFormatException) {
        return BigDecimal.valueOf(default)
    }
}