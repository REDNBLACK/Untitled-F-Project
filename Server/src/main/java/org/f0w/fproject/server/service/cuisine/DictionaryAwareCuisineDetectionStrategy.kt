package org.f0w.fproject.server.service.cuisine

import mu.KLogging
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.QueryBuilders
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
        val boolFilter = QueryBuilders.boolQuery()

//        boolFilter.(QueryBuilders.termsQuery("food", *tokens))
        val boolQuery = QueryBuilders.boolQuery()

        tokens.forEach { boolFilter.should(QueryBuilders.fuzzyQuery("food", it).fuzziness(Fuzziness.ONE)) }
        boolQuery.filter(boolFilter)

        val x = elastic.prepareSearch(Constants.ELASTIC_CUISINE_INDEX)
                .setTypes(Constants.CUISINE_MAPPING)
                .setQuery(boolQuery)

        val y = x.get()

        logger.info { x }
        logger.info { y }

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