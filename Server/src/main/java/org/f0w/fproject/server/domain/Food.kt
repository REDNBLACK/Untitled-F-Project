package org.f0w.fproject.server.domain

import java.math.BigDecimal
import java.time.LocalTime
import java.time.Period

data class Food(
    /**
     * Название ресторана
     */
    val restaurantName: String,

    /**
     * Название службы доставки
     */
    val supplierName: String,

    /**
     * Город в который доставляется
     */
    val supplyingCity: String,

    /**
     * Районы в которые доставляется
     */
    val supplyingArea: List<String>,

    /**
     * Стоимость доставки
     */
    val supplyCost: BigDecimal,

    /**
     * Среднее время доставки
     */
    val supplyAvgTime: LocalTime,

    /**
     * Интервал приема заказов
     */
    val orderPeriod: Period,

    /**
     * Название блюда
     */
    val title: String,

    /**
     * Тип кухни
     */
    val cuisineType: String,

    /**
     * Минимальная сумма заказа]
     */
    val minimalCostAllowed: BigDecimal,

    /**
     * Стоимость блюда
     */
    val cost: BigDecimal,

    /**
     * Вес блюда
     */
    val weight: Double,

    /**
     * Описание блюда
     */
    val description: String,

    /**
     * Изображение блюда
     */
    val imageUUID: String,

    /**
     * Тэги блюда
     */
    val tags: List<String>
)