package org.f0w.fproject.server.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal

@ApiModel
data class SearchRequest(
    @ApiModelProperty(value = "Город в котором будет производиться поиск", required = true)
    val city: String,

    @ApiModelProperty(value = "Район города в котором будет производиться поиск", required = true)
    val area: String,

    @ApiModelProperty(value = "Фильтр по типу кухни", required = false)
    val cuisine: String,

    @ApiModelProperty(value = "Количество персон", required = true)
    val numberOfPersons: Int,

    @ApiModelProperty(value = "Максимальная стоимость заказа", required = true)
    val cost: BigDecimal
)
