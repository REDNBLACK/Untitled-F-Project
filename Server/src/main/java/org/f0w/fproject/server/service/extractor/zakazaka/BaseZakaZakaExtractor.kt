package org.f0w.fproject.server.service.extractor.zakazaka

import com.google.common.base.CharMatcher
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import mu.KLogging
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.service.extractor.AbstractExtractor
import org.f0w.fproject.server.service.extractor.ExtractionException
import org.f0w.fproject.server.utils.containsAll
import org.f0w.fproject.server.utils.toBigDecimalOrEmpty
import org.f0w.fproject.server.utils.toBigDecimalOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.math.BigDecimal
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.concurrent.TimeUnit

abstract class BaseZakaZakaExtractor(protected val title: String, val client: OkHttpClient) : AbstractExtractor() {
    companion object: KLogging() {
        val SUPPLIER_NAME = "ZakaZaka"
        val WEIGHT_REGEX = Regex("(\\d{1,9}+)(\\s*)гр")
    }

    protected val baseUrl = "https://spb.zakazaka.ru"
    protected val menuUrl = "$baseUrl/restaurants/menu/$title"
    protected val infoUrl = "$baseUrl/restaurants/info/$title"
    private val parser = Parser()

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
                .skip(5)
                .limit(1)
                .map { href -> Request.Builder().url(href).build() }
                .zipWith(Observable.interval(3, TimeUnit.SECONDS), { request, interval -> request })
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), baseUrl) }
                .zipWith(info.repeat(), { menu, info ->
                    menu.select("#contentBox").append(info.select("#contentBox").html())

                    return@zipWith menu
                })
                .flatMap { parseEntries(it) }
    }

    protected fun parseEntries(document: Document): Observable<Food> {
        val restaurantName = parser.parseRestaurantName(document)
        val supplyingCity = parser.parseSupplyingCity(document)
        val supplyAvgTime = parser.parseSupplyAvgTime(document)
        val supplyCost = parser.parseSupplyCost(document)
        val supplierName = parser.parseSupplierName(document)
        val minimalCostAllowed = parser.parseMinimalCostAllowed(document)
        val (orderPeriodStart, orderPeriodEnd) = parser.parseOrderPeriod(document)

        val supplyingArea = emptyList<String>()
        val cuisineType = ""
        val tags = emptyList<String>()

        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".product-item")) {
                    val cost = parser.parseProductCost(product)

                    if (cost == null) {
                        logger.warn { "Не удалось получить цену блюда!" }
                        continue
                    }

                    subscriber.onNext(Food(
                            restaurantName = restaurantName,
                            supplierName = supplierName,
                            supplyingCity = supplyingCity,
                            supplyingArea = supplyingArea,
                            supplyCost = supplyCost,
                            supplyAvgTime = supplyAvgTime,
                            orderPeriodStart = orderPeriodStart,
                            orderPeriodEnd = orderPeriodEnd,
                            title = parser.parseProductTitle(product),
                            cuisineType = cuisineType,
                            minimalCostAllowed = minimalCostAllowed,
                            cost = cost,
                            weight = parser.parseProductWeight(product),
                            description = parser.parseProductDescription(product),
                            imageUUID = parser.parseProductImage(product),
                            tags = tags
                    ))
                }

                subscriber.onCompleted()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    private class Parser {
        companion object {
            val SMART_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.SMART)
        }

        fun parseRestaurantName(root: Document): String {
            return root.select(".restoran-item_title")
                    .text()
                    .let { if (it.isEmpty()) null else it }
                    ?: throw ExtractionException("Не удалось получить название ресторана!")
        }

        fun parseSupplyingCity(root: Document): String {
            return root.select("#current-city")
                    .text()
                    .capitalize()
                    .let { if (it.isEmpty()) null else it }
                    ?: throw ExtractionException("Не удалось получить город доставки!")
        }

        fun parseSupplyAvgTime(root: Document): LocalTime {
            return root.select(".sprite-ico-timer-2")
                    ?.first()
                    ?.parent()
                    ?.text()
                    ?.let {
                        when {
                            it.contains("мин") -> LocalTime.of(0, CharMatcher.DIGIT.retainFrom(it).toInt())
                            it.contains("час") -> LocalTime.of(CharMatcher.DIGIT.retainFrom(it).toInt(), 0)
                            else -> null
                        }
                    }
                    ?: throw ExtractionException("Не удалось получить среднее время доставки!")
        }

        fun parseSupplyCost(root: Document): BigDecimal {
            return root.select(".sprite-ico-rocket-w")
                    ?.first()
                    ?.parent()
                    ?.text()
                    .toBigDecimalOrEmpty()
        }

        fun parseSupplierName(root: Document): String {
            return when {
                (parseSupplyCost(root).signum() == 0) -> parseRestaurantName(root)
                else -> SUPPLIER_NAME
            }
        }

        fun parseMinimalCostAllowed(root: Document): BigDecimal {
            return root.select(".need_minimum_summa")
                    ?.first()
                    ?.attr("data-summa")
                    .toBigDecimalOrEmpty()
        }

        fun parseOrderPeriod(root: Document): Pair<LocalTime, LocalTime> {
            return root.select(".notification--about span")
                    ?.first()
                    ?.text()
                    ?.let {
                        if (!it.containsAll(":", "-")) return@let null

                        val (start, end) = it.split("-");

                        Pair(LocalTime.parse(start, SMART_TIME_FORMATTER), LocalTime.parse(end, SMART_TIME_FORMATTER))
                    }
                    ?: throw ExtractionException("Не удалось получить режим работы ресторана!")
        }

        fun parseProductCost(node: Element): BigDecimal? {
            return node.select(".product-item_bonus span")
                    ?.first()
                    ?.text()
                    .toBigDecimalOrNull()
        }

        fun parseProductTitle(node: Element): String {
            return node.select(".product-item_title p")
                    ?.first()
                    ?.text()
                    ?: throw ExtractionException("Не удалось получить название блюда!")
        }

        fun parseProductDescription(node: Element): String? {
            return node.select(".ingredients p")
                    ?.first()
                    ?.text()
        }

        fun parseProductImage(node: Element): String? {
            return node.select(".product-item_image img")
                    ?.first()
                    ?.absUrl("src")
        }

        fun parseProductWeight(node: Element): Double? {
            return parseProductTitle(node).plus(parseProductDescription(node))
                    .let { WEIGHT_REGEX.find(it)?.groups?.get(1)?.value }
                    .toBigDecimalOrNull()
                    ?.toDouble()
        }
    }
}
