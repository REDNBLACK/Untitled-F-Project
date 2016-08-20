package org.f0w.fproject.server.service.cuisine

/**
 * Стратегия с захардкоженым типом кухни, всегда возвращает переданное в конструктор.
 */
class StaticCuisineDetectionStrategy(val cuisineType: String) : CuisineDetectionStrategy {
    /**
     * {@inheritDoc}
     */
    override fun detect(foodTitle: String): String {
        return cuisineType
    }
}