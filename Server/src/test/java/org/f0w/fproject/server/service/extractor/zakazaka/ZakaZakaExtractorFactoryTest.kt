package org.f0w.fproject.server.service.extractor.zakazaka

import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class ZakaZakaExtractorFactoryTest {
    @Autowired
    private lateinit var factory: ZakaZakaExtractorFactory

    @Test
    fun make() {
        val extractor = factory.make("Brynza")

        println(extractor.restaurantLink)
        println(extractor.supplyingArea)
        println(extractor.cuisineDetectionStrategy)
    }
}