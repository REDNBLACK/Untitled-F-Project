package org.f0w.fproject.server.service.extractor.deliveryclub;

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

class DeliveryClubExtractor(
        private val restaurantName: String,
        private val restaurantLink: String,
        private val supplyingArea: List<String>,
        private val client: OkHttpClient
) : AbstractExtractor() {
    companion object: KLogging() {
        const val SUPPLIER_NAME = "Delivery Club"
        const val BASE_URL = "http://spb.delivery-club.ru"
    }

    private val menuUrl = "$BASE_URL/srv/$restaurantLink"
    private val infoUrl = "$menuUrl/info"
    private val parser = Parser(restaurantName)

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
                .zipWith(info.repeat(), { menu, info ->
                    menu.select("#content").append(info.select("#content").html())

                    return@zipWith menu
                })
                .flatMap { extractEntries(it) }
    }

    private fun extractEntries(document: Document): Observable<Food> {
        return Observable.create<Food> { subscriber ->
            try {
                for (product in document.select(".dish")) {
                    val cost = parser.parseProductCost(product)

                    if (cost == null) {
                        logger.warn { "Не удалось получить цену блюда для: $product" }
                        continue
                    }

                    val (orderPeriodStart, orderPeriodEnd) = parser.parseOrderPeriod(document)
                    val tags = emptyList<String>()

                    val food = Food(
                            restaurantName = restaurantName,
                            supplierName = parser.parseSupplierName(document),
                            supplyingCity = parser.parseSupplyingCity(document),
                            supplyingArea = supplyingArea,
                            supplyCost = parser.parseSupplyCost(document),
                            supplyAvgTime = parser.parseSupplyAvgTime(document),
                            orderPeriodStart = orderPeriodStart,
                            orderPeriodEnd = orderPeriodEnd,
                            title = parser.parseProductTitle(product),
                            cuisineType = parser.parseCuisineType(document),
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

    private class Parser(private val restaurantName: String) {
        companion object {
            val WEIGHT_REGEX = Regex("(\\d{1,9}+)(\\s*)гр")
            val SMART_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
                    .withResolverStyle(ResolverStyle.SMART)
            val TIME_MATCHER: CharMatcher = CharMatcher.DIGIT
                    .or(CharMatcher.`is`(','))
                    .precomputed()
        }

        fun parseSupplyingCity(root: Document): String {
            return root.select("#user-addr__input")
                    ?.`val`()
                    ?.trim()
                    ?.split(",")
                    ?.first()
                    ?.capitalize()
                    ?.let { if (it.isEmpty()) null else it }
                    ?: throw ExtractionException("Не удалось получить город доставки!")
        }

        fun parseSupplyAvgTime(root: Document): LocalTime {
            val time = root.select(".placeholder_avg_time")
                    ?.first()
                    ?.text()
                    ?: throw ExtractionException("Не удалось получить среднее время доставки!")

            return when {
                time.contains("мин") -> {
                    val timeParsed = TIME_MATCHER.retainFrom(time).toInt()
                    val hours = if (timeParsed >= 60) timeParsed / 60 else 0
                    val minutes = if (timeParsed < 60) timeParsed else 0

                    return LocalTime.of(hours, minutes)
                }
                time.contains("час") -> try {
                    val hours = TIME_MATCHER.retainFrom(time)
                            .replace(",", ".")
                            .toDouble()

                    val minutes = (hours % 1) * 60 / 1

                    LocalTime.of(hours.toInt(), minutes.toInt())
                } catch (e: NumberFormatException) {
                    throw ExtractionException("Не удалось получить среднее время доставки!", e)
                }
                else -> throw ExtractionException("Не удалось получить среднее время доставки!")
            }
        }

        fun parseSupplyCost(root: Document): BigDecimal {
            return root.select(".placeholder_delivery_cost")
                    ?.first()
                    ?.text()
                    ?.trim()
                    ?.let { if (it == "БЕСПЛАТНО") null else it }
                    .toBigDecimalOrEmpty()
        }

        fun parseSupplierName(root: Document): String {
            return when {
                (parseSupplyCost(root).signum() == 0) -> restaurantName
                else -> SUPPLIER_NAME
            }
        }

        fun parseCuisineType(root: Document): List<String> {
            return root.select(".category span")
                    ?.first()
                    ?.text()
                    ?.replace("/", "")
                    ?.replace(",", "")
                    ?.split(" ")
                    ?.map { it.trim() }
                    ?.filter { !it.isNullOrEmpty() }
                    ?: throw ExtractionException("Не удалось получить тип кухни!")
        }

        fun parseMinimalCostAllowed(root: Document): BigDecimal {
            return root.select(".placeholder_min_order")
                    ?.first()
                    ?.ownText()
                    .toBigDecimalOrEmpty()
        }

        fun parseOrderPeriod(root: Document): Pair<LocalTime, LocalTime> {
            return root.select(".placeholder_workhours")
                    ?.first()
                    ?.text()
                    ?.let {
                        if (it == "круглосуточно") return@let Pair(LocalTime.MIN, LocalTime.MAX)

                        if (!it.containsAll(":", "до")) return@let null

                        val (start, end) = it.replace("с", "").split("до").map { it.trim() }

                        return Pair(
                                LocalTime.parse(start, SMART_TIME_FORMATTER),
                                LocalTime.parse(end, SMART_TIME_FORMATTER)
                        )
                    }
                    ?: throw ExtractionException("Не удалось получить режим работы ресторана!")
        }

        fun parseProductCost(node: Element): BigDecimal? {
            return node.select("form > p > strong > span")
                    ?.first()
                    ?.text()
                    .toBigDecimalOrNull()
        }

        fun parseProductTitle(node: Element): String {
            return node.select(".product_title span")
                    ?.first()
                    ?.text()
                    ?: throw ExtractionException("Не удалось получить название блюда!")
        }

        fun parseProductDescription(node: Element): String? {
            return node.select(".dish_detail p")
                    ?.first()
                    ?.text()
        }

        fun parseProductImage(node: Element): String? {
            return node.select(".main_img img")
                    ?.first()
                    ?.let { "$BASE_URL${it.attr("data-load")}" }
        }

        fun parseProductWeight(node: Element): Double? {
            return parseProductTitle(node).plus(parseProductDescription(node))
                    .let { WEIGHT_REGEX.find(it)?.groups?.get(1)?.value }
                    .toBigDecimalOrNull()
                    ?.toDouble()
        }
    }
}
