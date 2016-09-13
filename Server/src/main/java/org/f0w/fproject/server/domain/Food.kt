package org.f0w.fproject.server.domain

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalTime

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
     * Минимальная сумма заказа
     */
    val minimalCostAllowed: BigDecimal,

    /**
     * Среднее время доставки
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    val supplyAvgTime: LocalTime,

    /**
     * Начало интервала приема заказов
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    val orderPeriodStart: LocalTime,

    /**
     * Конец интервала приема заказов
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    val orderPeriodEnd: LocalTime,

    /**
     * Название блюда
     */
    val title: String,

    /**
     * Тип кухни
     */
    val cuisineType: List<String>,

    /**
     * Стоимость блюда
     */
    val cost: BigDecimal,

    /**
     * Вес блюда
     */
    val weight: Double?,

    /**
     * Описание блюда
     */
    val description: String?,

    /**
     * Изображение блюда
     */
    val imageUUID: String?,

    /**
     * Тэги блюда
     */
    val tags: List<String>
)