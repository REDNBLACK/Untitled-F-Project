package org.f0w.fproject.server.service

import org.elasticsearch.client.Client
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class SearchHintServiceTest {
    @Autowired
    lateinit var elastic: Client

    @Test
    fun test() {
        println(SearchHintService(elastic).hintCity())
        println(SearchHintService(elastic).hintAreas())
        println(SearchHintService(elastic).hintAreas("Санкт-Петербург"))
        println(SearchHintService(elastic).hintCuisines())
    }
}