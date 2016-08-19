package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.elasticsearch.client.Client
import org.f0w.fproject.server.service.cuisine.DictionaryAwareCuisineDetectionStrategy
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.concurrent.TimeUnit

@RunWith(SpringJUnit4ClassRunner::class)
open class OllisExtractorTest {
    @Autowired
    lateinit var elastic: Client

    @Test
    fun extract() {
//        val count = AtomicInteger(0)
//
////        areas.forEach { println { it } }
//
        OllisExtractor(OkHttpClient())
                .extract()
//                .doOnNext { count.incrementAndGet() }
//        .subscribe()
//                .map { it.title }
                .subscribe(
                        { println(it) },
                        { println(it) }
                )
//
//        println("count: $count")

        DictionaryAwareCuisineDetectionStrategy(elastic).detect("Пицца перфекто оллис молодежная 417 гр. *");
//
        TimeUnit.HOURS.sleep(2)
    }
}