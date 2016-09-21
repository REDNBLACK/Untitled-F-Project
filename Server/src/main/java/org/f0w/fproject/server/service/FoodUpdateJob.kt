package org.f0w.fproject.server.service

import com.fasterxml.jackson.databind.ObjectWriter
import mu.KLogging
import org.elasticsearch.client.Client
import org.f0w.fproject.server.Constants
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.service.extractor.ExtractorFactory
import org.f0w.fproject.server.utils.DomainException
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
//import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class FoodUpdateJob(
        private val restaurants: Set<String>,
        private val extractorFactory: ExtractorFactory,
        private val elastic: Client,
        private val jsonWriter: ObjectWriter
//        private val threads: Int,
//        private val terminationInterval: Pair<TimeUnit, Long>
) : Runnable {
    companion object: KLogging()

    override fun run() {
        logger.info("""
            Запущена обработка сервиса ${extractorFactory.getName()}
            Для ресторанов: $restaurants
            """.trimIndent()
        )

        val foodCount = AtomicLong(0)
        val executor = Executors.newSingleThreadExecutor()

        executor.submit {
            Observable.from(restaurants)
                    .map { extractorFactory.make(it) }
                    .flatMap { it.extract() }
                    .doOnNext { foodCount.incrementAndGet() }
                    .buffer(100)
                    .subscribe(
                            { foodList -> insertToElastic(foodList) },
                            { error -> logger.error("Ошибка сохранения блюда", error) },
                            {
                                logger.info("""
                                    Сохранение блюд было успешно завершено.
                                    Рестораны (${restaurants.size}): $restaurants
                                    Всего блюд: $foodCount
                                    """.trimIndent()
                                )
                            }
                    )
        }

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.HOURS)
    }

    private fun insertToElastic(food: List<Food>) {
        val bulk = elastic.prepareBulk()

        food
            .map { jsonWriter.writeValueAsString(it) }
            .map { foodAsJson -> elastic.prepareIndex()
                    .setIndex(Constants.FOOD_INDEX)
                    .setType(Constants.FOOD_TYPE)
                    .setSource(foodAsJson)
            }
            .forEach { bulk.add(it) }

        val response = bulk.get()

        if (response.hasFailures()) {
            throw DomainException(response.buildFailureMessage())
        }
    }
}
