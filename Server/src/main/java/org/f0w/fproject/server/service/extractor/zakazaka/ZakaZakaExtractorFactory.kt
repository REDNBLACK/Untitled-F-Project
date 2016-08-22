package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.elasticsearch.client.Client
import org.f0w.fproject.server.service.cuisine.CuisineDetectionStrategy
import org.f0w.fproject.server.service.cuisine.DictionaryAwareCuisineDetectionStrategy
import org.f0w.fproject.server.service.cuisine.StaticCuisineDetectionStrategy
import org.f0w.fproject.server.utils.streamFromResources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.io.File

@Component
open class ZakaZakaExtractorFactory(
        @Autowired val yamlMapper: Yaml,
        @Autowired val elastic: Client,
        @Autowired val httpClient: OkHttpClient
) {
    open fun make(restaurant: String): ZakaZakaExtractor {
        val document = (yamlMapper.load(File("providers/ZakaZaka.yml").streamFromResources()) as Map<String, Any>)
        val restaurantData = document.get(restaurant) as Map<String, Any>
        val restaurantLink = restaurantData.get("restaurantLink") as String
        val supplyingArea = getSupplyingArea(restaurantData.get("supplyingArea") as Map<String, Any>)
        val cuisineDetectionStrategy = getCuisineDetectionStrategy(
                restaurantData.get("cuisineDetectionStrategy") as Map<String, Any>
        )

        return ZakaZakaExtractor(restaurantLink, supplyingArea, cuisineDetectionStrategy, httpClient)
    }

    private fun getCuisineDetectionStrategy(cuisineDetectionStrategy: Map<String, Any>): CuisineDetectionStrategy {
        val type = cuisineDetectionStrategy.get("type") as String

        when (type) {
            "static" -> return StaticCuisineDetectionStrategy(cuisineDetectionStrategy.get("contents") as String)
            "dictionary" -> return DictionaryAwareCuisineDetectionStrategy(elastic)
            else -> throw IllegalArgumentException("Неподдерживаемый формат стратегии поиска типа кухни!")
        }
    }

    private fun getSupplyingArea(supplyingArea: Map<String, Any>): List<String> {
        val type = supplyingArea.get("type") as String
        val contents = supplyingArea.get("contents")

        when (type) {
            "file" -> return yamlMapper.load(File("areas/${contents as String}.yml").streamFromResources()) as List<String>
            "list" -> return contents as List<String>
            else -> throw IllegalArgumentException("Неподдерживаемый формат района доставки!")
        }
    }
}