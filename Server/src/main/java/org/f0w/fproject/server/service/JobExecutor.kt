package org.f0w.fproject.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.Client
import org.f0w.fproject.server.Application
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.service.extractor.ExtractorFactory
import org.f0w.fproject.server.service.extractor.ExtractorsFactory
import org.f0w.fproject.server.utils.streamFromResources
import org.f0w.fproject.server.utils.toStringFromResources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

@Service
class JobExecutor(
        @Autowired
        private val env: Environment,

        @Autowired
        private val elastic: Client,

        @Autowired
        private val extractorsFactory: ExtractorsFactory,

        @Autowired
        private val yaml: Yaml,

        @Autowired
        private val jsonMapper: ObjectMapper
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val working = AtomicBoolean(false)

    @PostConstruct
    fun init() {
        if (!env.getProperty("food-extractor.force-update", Boolean::class.java, false)) return

        val jobs = arrayListOf(
            makeJob(
                (yaml.load(File("providers/ZakaZaka.yml").streamFromResources()) as Map<String, Any>).keys,
                extractorsFactory.getZakaZakaExtractorFactory()
            )
        )

        update(jobs)
    }

    /**
     * Создает Job на основе данных
     */
    private fun makeJob(restaurants: Set<String>, extractorFactory: ExtractorFactory): FoodUpdateJob {
        return FoodUpdateJob(
                restaurants = restaurants,
                extractorFactory = extractorFactory,
                elastic = elastic,
                jsonWriter = jsonMapper.writer()
        )
    }

    /**
     * Запуск процесса обновления
     */
    private fun update(jobs: List<FoodUpdateJob>) {
        if (elastic.admin().indices().prepareExists(Constants.ELASTIC_FOOD_INDEX).get().isExists) {
            elastic.admin()
                    .indices()
                    .prepareDelete(Constants.ELASTIC_FOOD_INDEX)
                    .get()
        }

        Application.logger.info { "[Elastic] Создание индекса ${Constants.ELASTIC_FOOD_INDEX}" }

        val mapping = File("elastic/food.index.json").toStringFromResources()

        elastic.admin()
                .indices()
                .prepareCreate(Constants.ELASTIC_FOOD_INDEX)
                .addMapping(Constants.FOOD, mapping)
                .get()

        if (working.get()) return else working.set(true)

        val list = jobs.map { CompletableFuture.runAsync(it, executor) }.toTypedArray()

        CompletableFuture.allOf(*list)
                .whenComplete({ v, t ->
                    working.set(false)
                    Application.logger.info("Обновление списка еды завершено")
                })
    }
}
