package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.elasticsearch.client.Client
import org.f0w.fproject.server.service.extractor.Extractor
import org.f0w.fproject.server.service.extractor.ExtractorFactory
import org.f0w.fproject.server.utils.streamFromResources
import org.yaml.snakeyaml.Yaml
import java.io.File

class ZakaZakaExtractorFactory(
        private val yaml: Yaml,
        private val elastic: Client,
        private val httpClient: OkHttpClient
) : ExtractorFactory {
    override fun getName() = "ZakaZaka"

    override fun make(restaurant: String): Extractor {
        val document = (yaml.load(File("providers/ZakaZaka.yml").streamFromResources()) as Map<String, Any>)
        val restaurantData = document.get(restaurant) as Map<String, Any>
        val restaurantLink = restaurantData.get("restaurantLink") as String
        val supplyingArea = getSupplyingArea(restaurantData.get("supplyingArea") as Map<String, Any>)

        return ZakaZakaExtractor(restaurant, restaurantLink, supplyingArea, httpClient)
    }

    private fun getSupplyingArea(supplyingArea: Map<String, Any>): List<String> {
        val type = supplyingArea.get("type") as String
        val contents = supplyingArea.get("contents")

        when (type) {
            "file" -> return yaml.load(File("areas/${contents as String}.yml").streamFromResources()) as List<String>
            "list" -> return contents as List<String>
            else -> throw IllegalArgumentException("Неподдерживаемый формат района доставки!")
        }
    }
}