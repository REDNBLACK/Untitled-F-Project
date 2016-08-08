package org.f0w.fproject.server;

import org.elasticsearch.client.Client;
import org.f0w.fproject.server.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Client elastic;

    @PostConstruct
    public void init() {
        if (!elastic.admin().indices().prepareExists(Constants.ELASTIC_FOOD_INDEX).get().isExists()) {
            LOG.info("[Elastic] Создание индекса {}", Constants.ELASTIC_FOOD_INDEX);
            String settings = IOUtils.readFileFromResources("elastic/food.index.json");
            elastic.admin()
                .indices()
                .prepareCreate(Constants.ELASTIC_FOOD_INDEX)
                .addMapping(Constants.FOOD, settings)
                .get();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
