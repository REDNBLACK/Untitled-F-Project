package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OllisExtractorTest {
    @Test
    fun extract() {
        val count = AtomicInteger(0)

        OllisExtractor(OkHttpClient())
                .extract()
                .doOnNext { count.incrementAndGet() }
//                .map { it.title }
                .subscribe(
                        { println(it) },
                        { println(it) }
                )

        println("count: $count")

        TimeUnit.HOURS.sleep(2)
    }
}