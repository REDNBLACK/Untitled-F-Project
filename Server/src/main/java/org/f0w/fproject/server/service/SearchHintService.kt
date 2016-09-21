package org.f0w.fproject.server.service

import mu.KLogging
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders.terms
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.domain.Food
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SearchHintService(@Autowired private val elastic: Client) {
    companion object: KLogging()

    fun hintCity(): Set<String> {
        val response = prepareRequest(boolQuery(), terms("aggs").field(Food::supplyingCity.name))
                .get()

        return aggregationsToSet(response.aggregations)
    }

    fun hintAreas(city: String? = null): Set<String> {
        val query = when (city) {
            null -> boolQuery()
            else -> boolQuery().filter(boolQuery().must(termQuery(Food::supplyingCity.name, city)))
        }

        val response = prepareRequest(query, terms("aggs").field(Food::supplyingArea.name))
            .get()

        return aggregationsToSet(response.aggregations)
    }

    fun hintCuisines(): Set<String> {
        val response = prepareRequest(boolQuery(), terms("aggs").field(Food::cuisineType.name))
            .get()

        return aggregationsToSet(response.aggregations)
    }

    private fun prepareRequest(query: QueryBuilder, aggregations: AbstractAggregationBuilder): SearchRequestBuilder {
        val requestBuilder = elastic.prepareSearch(Constants.FOOD_INDEX)
                .setQuery(query)
                .setSize(0)
                .addAggregation(aggregations)

        logger.debug { "Request: $requestBuilder" }

        return requestBuilder
    }

    private fun aggregationsToSet(aggregations: Aggregations): Set<String> {
        return aggregations.first()
                ?.let { it as? StringTerms }
                ?.buckets
                ?.map { it.keyAsString }
                ?.toSet()
                ?: setOf()
    }
}