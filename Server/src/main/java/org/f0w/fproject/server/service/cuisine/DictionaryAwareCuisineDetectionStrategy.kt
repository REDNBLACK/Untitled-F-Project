package org.f0w.fproject.server.service.cuisine

import mu.KLogging
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.index.query.QueryBuilders.fuzzyQuery
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.utils.DomainException

/**
 * Интеллектуальный поиск типа блюда по словарю
 */
class DictionaryAwareCuisineDetectionStrategy(private val elastic: Client) : CuisineDetectionStrategy {
    companion object: KLogging()

    /**
     * {@inheritDoc}
     */
    override fun detect(foodTitle: String): String {
        if (!isIndexAvailable()) {
            throw DomainException("Elastic недоступен")
        }

        val tokens = foodTitle.split(" ").map { it.toLowerCase() }

        val request = elastic.prepareSearch(Constants.ELASTIC_CUISINE_INDEX)
                .setTypes(Constants.CUISINE_MAPPING)
                .setQuery(buildQuery(tokens))

        val response = request.get()

//        logger.debug { request }
//        logger.debug { response }

        if (response.hits.totalHits() > 0) {
            return response.hits.first().source.get("cuisineType").toString()
        }

        return "Неизвестный тип кухни"
    }

    private fun buildQuery(tokens: List<String>): BoolQueryBuilder {
        val boolQuery = boolQuery()
        val boolFilter = boolQuery()
        tokens.forEach { boolFilter.should(fuzzyQuery("food", it).fuzziness(Fuzziness.ONE)) }
        boolQuery.filter(boolFilter)

        return boolQuery
    }

    private fun isIndexAvailable(): Boolean {
        return elastic.admin()
                .indices()
                .prepareExists(Constants.ELASTIC_CUISINE_INDEX)
                .get()
                .isExists
    }
}