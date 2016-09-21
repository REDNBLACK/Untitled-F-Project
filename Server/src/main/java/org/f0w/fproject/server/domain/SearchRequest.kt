package org.f0w.fproject.server.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal

@ApiModel
data class SearchRequest(
    @ApiModelProperty(value = "Город в котором будет производиться поиск", required = true)
    val city: String? = null,

    @ApiModelProperty(value = "Район города в котором будет производиться поиск", required = true)
    val area: String? = null,

    @ApiModelProperty(value = "Фильтр по типу кухни", required = false)
    val cuisine: String? = null,

    @ApiModelProperty(value = "Количество персон", required = true)
    val numberOfPersons: Int? = null,

    @ApiModelProperty(value = "Максимальная стоимость заказа", required = true)
    val maxCost: BigDecimal? = null
)
