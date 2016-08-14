package org.f0w.fproject.server.service.extractor.zakazaka

import com.google.common.base.CharMatcher
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import mu.KLogging
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.service.extractor.AbstractExtractor
import org.f0w.fproject.server.utils.toBigDecimalOrEmpty
import org.f0w.fproject.server.utils.toBigDecimalOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import rx.Observable
import java.math.BigDecimal
import java.time.LocalTime
import java.time.Period

abstract class BaseZakaZakaExtractor(protected val title: String, val client: OkHttpClient) : AbstractExtractor() {
    companion object: KLogging()

    protected val baseUrl = "https://spb.zakazaka.ru"
    protected val menuUrl = "$baseUrl/restaurants/menu/$title"
    protected val infoUrl = "$baseUrl/restaurants/info/$title"

    protected val emptyCost = BigDecimal.valueOf(0.0)

    fun extract(): Observable<Food> {
        return traverseMenu()
    }

    protected fun traverseMenu(): Observable<Food> {
        val infoObservable = Observable.just(infoUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }

        val menuObservable = Observable.just(menuUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }

        return Observable.just(menuUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .map { it.select(".sort-block_content a") }
                .flatMap { Observable.from(it) }
                .map { it.absUrl("href") }
                .limit(1)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .flatMap { parseEntries(it) }
    }

    protected fun parseEntries(document: Document): Observable<Food> {
        val restaurantName = document.select(".restoran-item_title").text()

        val supplyingCity = document.select("#current-city").text().capitalize()

        val supplyAvgTime = document.select(".sprite-ico-timer-2")
                ?.first()
                ?.parent()
                ?.text()
                ?.let {
                    when {
                        it.contains("мин") -> LocalTime.of(0, CharMatcher.DIGIT.retainFrom(it).toInt())
                        it.contains("час") -> LocalTime.of(CharMatcher.DIGIT.retainFrom(it).toInt(), 0)
                        else -> LocalTime.MIN
                    }
                }
                ?: LocalTime.MIN

        val supplyCost = document.select(".sprite-ico-rocket-w")
                ?.first()
                ?.parent()
                ?.text()
                ?.toBigDecimalOrEmpty()
                ?: emptyCost

        val supplierName = when {
            supplyCost.equals(emptyCost) -> restaurantName
            else -> "ZakaZaka"
        }

        val minimalCostAllowed = document.select(".need_minimum_summa")
                ?.first()
                ?.attr("data-summa")
                ?.toBigDecimalOrEmpty()
                ?: emptyCost

        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".product-item")) {
                    val cost = product.select(".product-item_bonus span")
                            ?.first()
                            ?.text()
                            ?.toBigDecimalOrNull()
                            ?: continue;

                    val food = Food(
                            restaurantName = restaurantName,
                            supplierName = supplierName,
                            supplyingCity = supplyingCity,
                            supplyingArea = emptyList<String>(),
                            supplyCost = BigDecimal.valueOf(0),
                            supplyAvgTime = supplyAvgTime,
                            orderPeriod = Period.ZERO,
                            title = product.select(".product-item_title p")?.first()?.text() ?: "",
                            cuisineType = "",
                            minimalCostAllowed = minimalCostAllowed,
                            cost = cost,
                            weight = 0.0,
                            description = product.select(".ingredients p").text(),
                            imageUUID = product.select(".product-item_image img").first()?.absUrl("src") ?: "",
                            tags = emptyList<String>()
                    )

                    subscriber.onNext(food)
                }

                subscriber.onCompleted()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}
