package org.f0w.fproject.server.service.extractor.zakazaka

import com.google.common.base.CharMatcher
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import mu.KLogging
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.service.extractor.AbstractExtractor
import org.f0w.fproject.server.utils.containsAll
import org.f0w.fproject.server.utils.toBigDecimalOrEmpty
import org.f0w.fproject.server.utils.toBigDecimalOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import rx.Observable
import java.math.BigDecimal
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.concurrent.TimeUnit

abstract class BaseZakaZakaExtractor(protected val title: String, val client: OkHttpClient) : AbstractExtractor() {
    companion object: KLogging()

    protected val baseUrl = "https://spb.zakazaka.ru"
    protected val menuUrl = "$baseUrl/restaurants/menu/$title"
    protected val infoUrl = "$baseUrl/restaurants/info/$title"

    protected val EMPTY_COST = BigDecimal.valueOf(0.0)
    protected val SMART_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.SMART)

    fun extract(): Observable<Food> {
        val info = Observable.just(infoUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .cache()

        return traverseMenu(info)
    }

    protected fun traverseMenu(info: Observable<Document>): Observable<Food> {
        return Observable.just(menuUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .map { it.select(".sort-block_content a") }
                .flatMap { Observable.from(it) }
                .map { it.absUrl("href") }
                .map { href -> Request.Builder().url(href).build() }
                .zipWith(Observable.interval(3, TimeUnit.SECONDS), { href, interval -> href })
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .zipWith(info.repeat(), { menu, info ->
                    menu.select("#contentBox").append(info.select("#contentBox").html())

                    return@zipWith menu
                })
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
                ?: EMPTY_COST

        val supplierName = when {
            supplyCost.equals(EMPTY_COST) -> restaurantName
            else -> "ZakaZaka"
        }

        val minimalCostAllowed = document.select(".need_minimum_summa")
                ?.first()
                ?.attr("data-summa")
                ?.toBigDecimalOrEmpty()
                ?: EMPTY_COST

        val (orderPeriodStart, orderPeriodEnd) = document.select(".notification--about span")
                ?.first()
                ?.text()
                ?.let {
                    if (!it.containsAll(":", "-")) return@let null

                    val (start, end) = it.split("-");

                    Pair(LocalTime.parse(start, SMART_TIME_FORMATTER), LocalTime.parse(end, SMART_TIME_FORMATTER))
                }
                ?: Pair(LocalTime.MIN, LocalTime.MAX)

        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".product-item")) {
                    val cost = product.select(".product-item_bonus span")
                            ?.first()
                            ?.text()
                            ?.toBigDecimalOrNull()
                            ?: continue;

                    val title = product.select(".product-item_title p")
                            ?.first()
                            ?.text()
                            ?: ""

                    val description = product.select(".ingredients p")
                            ?.first()
                            ?.text()
                            ?: ""

                    val imageUUID = product.select(".product-item_image img")
                            ?.first()
                            ?.absUrl("src")
                            ?: ""

                    val supplyingArea = emptyList<String>()

                    val cuisineType = ""

                    val weight = 0.0;

                    val tags = emptyList<String>()

                    subscriber.onNext(Food(
                            restaurantName = restaurantName,
                            supplierName = supplierName,
                            supplyingCity = supplyingCity,
                            supplyingArea = supplyingArea,
                            supplyCost = supplyCost,
                            supplyAvgTime = supplyAvgTime,
                            orderPeriodStart = orderPeriodStart,
                            orderPeriodEnd = orderPeriodEnd,
                            title = title,
                            cuisineType = cuisineType,
                            minimalCostAllowed = minimalCostAllowed,
                            cost = cost,
                            weight = weight,
                            description = description,
                            imageUUID = imageUUID,
                            tags = tags
                    ))
                }

                subscriber.onCompleted()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}
