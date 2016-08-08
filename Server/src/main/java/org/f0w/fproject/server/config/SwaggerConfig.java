package org.f0w.fproject.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@Profile("!prod")
@EnableSwagger2
public class SwaggerConfig {
    @Autowired
    private Environment env;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(env.getProperty("spring.application.name"))
                .apiInfo(new ApiInfo(
                        env.getProperty("spring.application.name"),
                        "Сервис для поиска еды",
                        getClass().getPackage().getImplementationVersion(),
                        null,
                        new Contact(
                                "f0w.org",
                                "https://github.com/REDNBLACK/Untitled-F-Project",
                                "rednblack@protonmail.com"
                        ),
                        null,
                        null
                ))
                .select()
                .paths(PathSelectors.regex("/api.*"))
                .build();
    }
}
