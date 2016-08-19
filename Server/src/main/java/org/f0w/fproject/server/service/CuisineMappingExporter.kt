package org.f0w.fproject.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.elasticsearch.client.Client
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.domain.CuisineMapping
import org.f0w.fproject.server.utils.DomainException
import org.f0w.fproject.server.utils.pointToResources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import javax.annotation.PostConstruct

@Component
class CuisineMappingExporter(
        @Autowired val elastic: Client,
        @Autowired val yamlMapper: Yaml,
        @Autowired val jsonMapper: ObjectMapper
) {
    companion object: KLogging()

    @PostConstruct
    fun init() {
        batchInsertToElastic(loadMappings())
    }

    private fun loadMappings(): List<CuisineMapping> {
        return File("cuisines")
                .pointToResources()
                .listFiles()
                .map { FileInputStream(it) }
                .map { yamlMapper.load(it) }
                .map {
                    when(it) {
                        is Map<*, *> -> it as Map<String, List<String>>
                        else -> null
                    }
                }
                .filterNotNull()
                .flatMap {
                    mapStruct -> mapStruct.flatMap {
                        mapEntry -> mapEntry.value.map { CuisineMapping(mapEntry.key, it) }
                    }
                }
    }

    private fun batchInsertToElastic(cuisineMappings: List<CuisineMapping>) {
        try {
            val bulk = elastic.prepareBulk()

            cuisineMappings
                    .map { jsonMapper.writeValueAsString(it) }
                    .map { cuisineMappingAsJson -> elastic.prepareIndex()
                            .setIndex(Constants.ELASTIC_CUISINE_INDEX)
                            .setType(Constants.CUISINE_MAPPING)
                            .setSource(cuisineMappingAsJson)
                    }
                    .forEach { bulk.add(it) }

            val response = bulk.get();

            if (response.hasFailures()) {
                throw DomainException(response.buildFailureMessage())
            }
        } catch (e: Exception) {
            logger.error { "[CuisineMappingExporter] Ошибка записи соотношений кухни и блюда $e" }
        }
    }
}