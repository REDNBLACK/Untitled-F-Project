package org.f0w.fproject.server.service.cuisine

interface CuisineDetectionStrategy {
    fun detect(foodTitle: String): String
}
