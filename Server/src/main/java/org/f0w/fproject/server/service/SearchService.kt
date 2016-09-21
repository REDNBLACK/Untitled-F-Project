package org.f0w.fproject.server.service

import mu.KLogging
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.domain.SearchRequest
import org.f0w.fproject.server.domain.SearchResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.elasticsearch.action.search.SearchResponse as ElasticSearchResponse

@Service
class SearchService(@Autowired private val elastic: Client) {
    companion object: KLogging()

    data class Restaurant(private val name: String)

    fun find(requestParams: SearchRequest): SearchResponse? {
        val request = prepareRequest(buildQuery(requestParams), addRestaurantNameAggregation())
        val response = prepareResponse(request.get())

        logger.info { response }

        return null
    }

    private fun buildQuery(request: SearchRequest): QueryBuilder {
        val filter = boolQuery()

        when {
            request.city != null -> filter.must(termQuery(Food::supplyingCity.name, request.city))
            request.area != null -> filter.must(termQuery(Food::supplyingArea.name, request.area))
            request.cuisine != null -> filter.must(termQuery(Food::cuisineType.name, request.cuisine))
        }

        return boolQuery().filter(filter)
    }

    private fun addRestaurantNameAggregation(): AbstractAggregationBuilder {
        return AggregationBuilders.terms("aggs").field(Food::restaurantName.name)
    }

    private fun prepareRequest(query: QueryBuilder, aggregations: AbstractAggregationBuilder): SearchRequestBuilder {
        val request = elastic.prepareSearch(Constants.FOOD_INDEX)
                .setQuery(query)
                .addAggregation(aggregations)

        logger.info { "Request: $request" }

        return request
    }

    private fun prepareResponse(response: ElasticSearchResponse): Set<Restaurant> {
        logger.info { "Response: $response" }

        return response.aggregations
            .first()
            ?.let { it as? StringTerms }
            ?.buckets
            ?.map { it.keyAsString }
            ?.map { Restaurant(it) }
            ?.toSet()
            ?: setOf()
    }
}
