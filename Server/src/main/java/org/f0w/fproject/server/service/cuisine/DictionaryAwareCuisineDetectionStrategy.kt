package org.f0w.fproject.server.service.cuisine

import org.elasticsearch.client.Client

/**
 * Интеллектуальный поиск типа блюда по словарю
 */
class DictionaryAwareCuisineDetectionStrategy(elastic: Client) : CuisineDetectionStrategy {
    /**
     * {@inheritDoc}
     */
    override fun detect(foodTitle: String?): String? {
        throw UnsupportedOperationException()
    }
}