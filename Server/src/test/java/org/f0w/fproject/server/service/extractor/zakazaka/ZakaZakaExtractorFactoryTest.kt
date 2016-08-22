package org.f0w.fproject.server.service.extractor.zakazaka

import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest
class ZakaZakaExtractorFactoryTest {
    @Autowired
    private lateinit var factory: ZakaZakaExtractorFactory

    @Test
    fun make() {
        val extractor = factory.make("Pizza Ollis")

        println(extractor.restaurantLink)
        println(extractor.supplyingArea)
        println(extractor.cuisineDetectionStrategy)

        extractor.extract()
            .subscribe(
                    { println(it) },
                    { println(it) }
            )

        TimeUnit.MINUTES.sleep(2)
    }
}