package org.f0w.fproject.server.service.extractor.deliveryclub

import com.squareup.okhttp.OkHttpClient
import mu.KLogging
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class DeliveryClubExtractorTest {
    companion object: KLogging()
    @Test
    fun extract() {
        DeliveryClubExtractor("Burger Ollis", "Burger_Ollis", listOf("Калиниский район"), OkHttpClient())
            .extract()
            .subscribe(
                    { logger.info { it } },
                    { logger.info { it } }
            )

        TimeUnit.HOURS.sleep(2)
    }

}