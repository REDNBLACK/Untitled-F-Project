package org.f0w.fproject.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.okhttp.OkHttpClient
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.yaml.snakeyaml.Yaml

@SpringBootApplication
@EnableScheduling
open class Application {
    companion object: KLogging() {
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
    open fun jsonMapper(): ObjectMapper {
        val jsonMapper = ObjectMapper()
        jsonMapper.findAndRegisterModules()

        return jsonMapper
    }
}
