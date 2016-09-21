package org.f0w.fproject.server.service

import org.elasticsearch.client.Client
import org.f0w.fproject.server.domain.SearchRequest
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class SearchServiceTest {
    @Autowired
    lateinit var elastic: Client

    @Test
    fun find() {
        SearchService(elastic).find(SearchRequest(city = "Санкт-Петербург"))
    }
}