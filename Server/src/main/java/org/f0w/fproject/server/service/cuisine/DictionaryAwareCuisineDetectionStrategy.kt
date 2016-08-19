package org.f0w.fproject.server.service.cuisine

import mu.KLogging
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.utils.DomainException

/**
 * Интеллектуальный поиск типа блюда по словарю
 */
class DictionaryAwareCuisineDetectionStrategy(val elastic: Client) : CuisineDetectionStrategy {
    companion object: KLogging()

    /**
     * {@inheritDoc}
     */
    override fun detect(foodTitle: String): String {
        if (!isIndexAvailable()) {
            throw DomainException("Elastic недоступен")
        }

        val tokens = foodTitle.split(" ").toList()
        val boolFilter = QueryBuilders.boolQuery()
        tokens.forEach { token -> boolFilter.must(QueryBuilders.termQuery("food", token)) }
        val boolQuery = QueryBuilders.boolQuery()
        boolQuery.filter(boolFilter)

        val x = elastic.prepareSearch(Constants.ELASTIC_CUISINE_INDEX)
                .setTypes(Constants.CUISINE_MAPPING)
                .setQuery(boolQuery)
                .get()

        logger.info { x }

        return ""
    }

    private fun isIndexAvailable(): Boolean {
        return elastic.admin()
                .indices()
                .prepareExists(Constants.ELASTIC_CUISINE_INDEX)
                .get()
                .isExists;
    }
}