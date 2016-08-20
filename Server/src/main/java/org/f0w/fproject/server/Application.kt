package org.f0w.fproject.server

import mu.KLogging
import org.elasticsearch.client.Client
import org.f0w.fproject.server.utils.toStringFromResources
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.yaml.snakeyaml.Yaml
import java.io.File
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableScheduling
open class Application {
    companion object: KLogging() {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }

    @Autowired
    private lateinit var elastic: Client

    @PostConstruct
    fun init() {
        if (!elastic.admin().indices().prepareExists(Constants.ELASTIC_FOOD_INDEX).get().isExists) {
            logger.info { "[Elastic] Создание индекса ${Constants.ELASTIC_FOOD_INDEX}" }

            val mapping = File("elastic/food.index.json").toStringFromResources()

            elastic.admin()
                    .indices()
                    .prepareCreate(Constants.ELASTIC_FOOD_INDEX)
                    .addMapping(Constants.FOOD, mapping)
                    .get()
        }
    }

    @Bean
    open fun yaml() = Yaml()
}
