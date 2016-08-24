package org.f0w.fproject.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.elasticsearch.client.Client
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.domain.CuisineMapping
import org.f0w.fproject.server.utils.DomainException
import org.f0w.fproject.server.utils.pointToResources
import org.f0w.fproject.server.utils.toStringFromResources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import javax.annotation.PostConstruct

@Service
class CuisineMappingExporter(
        @Autowired val elastic: Client,
        @Autowired val yamlMapper: Yaml,
        @Autowired val jsonMapper: ObjectMapper
) {
    companion object: KLogging()

    @PostConstruct
    fun init() {
        dropElasticIndex()
        createElasticIndex()

        val cuisineMappings = loadMappings()

        insertToElastic(cuisineMappings)

        logger.info("[CuisineMappingExporter] Соотношения кухни к блюдам успешно обновлены (Кол-во записей: {})",
                cuisineMappings.size
        )
    }

    private fun loadMappings(): List<CuisineMapping> {
        return File("cuisines")
                .pointToResources()
                .listFiles()
                .map { FileInputStream(it) }
                .map { yamlMapper.load(it) }
                .map {
                    when (it) {
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

    private fun dropElasticIndex() {
        if (elastic.admin().indices().prepareExists(Constants.ELASTIC_CUISINE_INDEX).get().isExists) {
            logger.info { "[CuisineMappingExporter] Очистка индекса ${Constants.ELASTIC_CUISINE_INDEX}" }

            elastic.admin()
                    .indices()
                    .prepareDelete(Constants.ELASTIC_CUISINE_INDEX)
                    .get()
        }
    }

    private fun createElasticIndex() {
        if (!elastic.admin().indices().prepareExists(Constants.ELASTIC_CUISINE_INDEX).get().isExists) {
            logger.info { "[CuisineMappingExporter] Создание индекса ${Constants.ELASTIC_CUISINE_INDEX}" }

            val mapping = File("elastic/cuisine_mapping.index.json").toStringFromResources()

            elastic.admin()
                    .indices()
                    .prepareCreate(Constants.ELASTIC_CUISINE_INDEX)
                    .addMapping(Constants.CUISINE_MAPPING, mapping)
                    .get()
        }
    }

    private fun insertToElastic(cuisineMappings: List<CuisineMapping>) {
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

            val response = bulk.get()

            if (response.hasFailures()) {
                throw DomainException(response.buildFailureMessage())
            }
        } catch (e: Exception) {
            logger.error { "[CuisineMappingExporter] Ошибка записи соотношений кухни и блюда $e" }
        }
    }
}