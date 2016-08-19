package org.f0w.fproject.server.service.extractor.zakazaka

import com.google.common.base.CharMatcher
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import mu.KLogging
import org.f0w.fproject.server.domain.Food
import org.f0w.fproject.server.service.cuisine.CuisineDetectionStrategy
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

abstract class BaseZakaZakaExtractor(
        val config: ZakaZakaConfig,
        val cuisineDetectionStrategy: CuisineDetectionStrategy,
        val client: OkHttpClient
) : AbstractExtractor() {
    companion object: KLogging() {
        const val SUPPLIER_NAME = "ZakaZaka"
    }

    protected val baseUrl = "https://spb.zakazaka.ru"
    protected val menuUrl = "$baseUrl/restaurants/menu/${config.restaurantName}"
    protected val infoUrl = "$baseUrl/restaurants/info/${config.restaurantName}"
    private val parser = Parser()

    override fun extract(): Observable<Food> {
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
                .flatMap { extractEntries(it) }
    }

    protected fun extractEntries(document: Document): Observable<Food> {
        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".product-item")) {
                    val cost = parser.parseProductCost(product)

                    if (cost == null) {
                        logger.warn { "Не удалось получить цену блюда!" }
                        continue
                    }

                    val (orderPeriodStart, orderPeriodEnd) = parser.parseOrderPeriod(document)
                    val title = parser.parseProductTitle(product);
                    val tags = emptyList<String>()

                    subscriber.onNext(Food(
                            restaurantName = parser.parseRestaurantName(document),
                            supplierName = parser.parseSupplierName(document),
                            supplyingCity = parser.parseSupplyingCity(document),
                            supplyingArea = config.supplyingArea,
                            supplyCost = parser.parseSupplyCost(document),
                            supplyAvgTime = parser.parseSupplyAvgTime(document),
                            orderPeriodStart = orderPeriodStart,
                            orderPeriodEnd = orderPeriodEnd,
                            title = title,
                            cuisineType = cuisineDetectionStrategy.detect(title),
                            minimalCostAllowed = parser.parseMinimalCostAllowed(document),
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
            val WEIGHT_REGEX = Regex("(\\d{1,9}+)(\\s*)гр")
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
