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

class ZakaZakaExtractor(
        private val restaurantLink: String,
        private val supplyingArea: List<String>,
        private val cuisineDetectionStrategy: CuisineDetectionStrategy,
        private val client: OkHttpClient
) : AbstractExtractor() {
    companion object: KLogging() {
        const val SUPPLIER_NAME = "ZakaZaka"
        const val BASE_URL = "https://spb.zakazaka.ru"
    }

    private val menuUrl = "$BASE_URL/restaurants/menu/$restaurantLink"
    private val infoUrl = "$BASE_URL/restaurants/info/$restaurantLink"
    private val parser = Parser()

    override fun extract(): Observable<Food> {
        val info = Observable.just(infoUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), BASE_URL) }
                .cache()

        return traverseMenu(info)
    }

    private fun traverseMenu(info: Observable<Document>): Observable<Food> {
        return Observable.just(menuUrl)
                .map { href -> Request.Builder().url(href).build() }
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), BASE_URL) }
                .map { it.select(".sort-block a") }
                .flatMap { Observable.from(it) }
                .map { it.absUrl("href") }
                .map { href -> Request.Builder().url(href).build() }
                .zipWith(Observable.interval(1, TimeUnit.SECONDS), { request, interval -> request })
                .map { request -> client.newCall(request).execute() }
                .map { response -> Jsoup.parse(response.body().string(), BASE_URL) }
                .zipWith(info.repeat(), { menu, info ->
                    menu.select("#contentBox").append(info.select("#contentBox").html())

                    return@zipWith menu
                })
                .flatMap { extractEntries(it) }
    }

    private fun extractEntries(document: Document): Observable<Food> {
        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".product-item")) {
                    val cost = parser.parseProductCost(product)

                    if (cost == null) {
                        logger.warn { "Не удалось получить цену блюда для: $product" }
                        continue
                    }

                    val (orderPeriodStart, orderPeriodEnd) = parser.parseOrderPeriod(document)
                    val title = parser.parseProductTitle(product)
                    val tags = emptyList<String>()

                    val food = Food(
                            restaurantName = parser.parseRestaurantName(document),
                            supplierName = parser.parseSupplierName(document),
                            supplyingCity = parser.parseSupplyingCity(document),
                            supplyingArea = supplyingArea,
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
                    )

                    subscriber.onNext(food)
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
            val SMART_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
                    .withResolverStyle(ResolverStyle.SMART)
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
            val time = root.select(".sprite-ico-timer-2")
                    ?.first()
                    ?.parent()
                    ?.text()
                    ?: throw ExtractionException("Не удалось получить среднее время доставки!")

            return when {
                time.contains("мин") -> LocalTime.of(0, CharMatcher.DIGIT.retainFrom(time).toInt())
                time.contains("час") -> try {
                    val hours = CharMatcher.DIGIT
                            .or(CharMatcher.`is`(','))
                            .retainFrom(time)
                            .replace(",", ".")
                            .toDouble()

                    val minutes = (hours % 1) * 60 / 1

                    return LocalTime.of(hours.toInt(), minutes.toInt())
                } catch (e: NumberFormatException) {
                    throw ExtractionException("Не удалось получить среднее время доставки!", e)
                }
                else -> throw ExtractionException("Не удалось получить среднее время доставки!")
            }
        }

        fun parseSupplyCost(root: Document): BigDecimal {
            return root.select(".sprite-ico-rocket-w")
                    ?.first()
                    ?.parent()
                    ?.text()
                    ?.split(" ")
                    ?.firstOrNull()
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
