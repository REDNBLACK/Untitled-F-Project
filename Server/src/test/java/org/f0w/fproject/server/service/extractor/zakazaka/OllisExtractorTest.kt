package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.elasticsearch.client.Client
import org.f0w.fproject.server.service.cuisine.DictionaryAwareCuisineDetectionStrategy
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.concurrent.TimeUnit

open class OllisExtractorTest {

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
//
        TimeUnit.HOURS.sleep(2)
    }
}