package org.f0w.fproject.server.service.cuisine

import org.elasticsearch.client.Client
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest
class DictionaryAwareCuisineDetectionStrategyTest {
    @Autowired
    private lateinit var elastic: Client

    @Test
    fun detect() {
        DictionaryAwareCuisineDetectionStrategy(elastic).detect("Пицца перфекто оллис молодежная 417 гр. *")
    }
}