package org.f0w.fproject.server.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@Profile("!prod")
@EnableSwagger2
class SwaggerConfig {
    @Autowired
    private lateinit var env: Environment

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .groupName(env.getProperty("spring.application.name"))
                .apiInfo(ApiInfo(
                    env.getProperty("spring.application.name"),
                    "Сервис для поиска еды",
                    javaClass.`package`.implementationVersion,
                    null,
                    Contact(
                            "f0w.org",
                            "https://github.com/REDNBLACK/Untitled-F-Project",
                            "rednblack@protonmail.com"),
                    null,
                    null)
                )
                .select()
                .paths(PathSelectors.regex("/api.*"))
                .build()
    }
}
