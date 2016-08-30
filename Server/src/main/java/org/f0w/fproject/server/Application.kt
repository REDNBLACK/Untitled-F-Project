package org.f0w.fproject.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.squareup.okhttp.OkHttpClient
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.yaml.snakeyaml.Yaml

@SpringBootApplication
@EnableScheduling
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }

    @Bean
    open fun yaml() = Yaml()

    @Bean
    open fun httpClient() = OkHttpClient()

    @Bean
    @Primary
    open fun jsonMapper(): ObjectMapper {
        val jsonMapper = ObjectMapper()
        jsonMapper.findAndRegisterModules()

        return jsonMapper
    }

    @Bean
    open fun csvMapper(): CsvMapper {
        val csvMapper = CsvMapper()
        csvMapper.findAndRegisterModules()

        return csvMapper
    }
}
