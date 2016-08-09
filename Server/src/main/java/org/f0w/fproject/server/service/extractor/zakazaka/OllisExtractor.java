package org.f0w.fproject.server.service.extractor.zakazaka;

import org.f0w.fproject.server.domain.Food;
import org.f0w.fproject.server.utils.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import rx.Observable;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.diffplug.common.base.Errors.rethrow;
import static org.slf4j.LoggerFactory.getLogger;

public class OllisExtractor extends BaseZakaZakaExtractor {
    private static final Logger LOG = getLogger(OllisExtractor.class);

    public static String ROOT_URL = "https://spb.zakazaka.ru";
    public static String MENU_URL = "https://spb.zakazaka.ru/restaurants/menu/ollis";

    public OllisExtractor() {
        super("https://spb.zakazaka.ru/restaurants/menu/ollis");
    }

    public Observable<Food> extract() {
        return traverseMenu();
    }

    protected Observable<Food> traverseMenu() {
        return Observable.just(MENU_URL)
                .map(href -> rethrow().get(() -> new URL(href)))
                .map(link -> rethrow().get(() -> Jsoup.parse(link, 3000)))
                .map(document -> document.select(".sort-block_content a[href]"))
                .flatMap(Observable::from)
                .map(href -> rethrow().get(() -> new URL(ROOT_URL + href.attr("href"))))
                .delay(3, TimeUnit.SECONDS)
                .map(link -> rethrow().get(() -> Jsoup.parse(link, 3000)))
                .flatMap(this::parseEntries);
    }

    protected Observable<Food> parseEntries(Document document) {
        final String restaurantName = document.select(".restoran-item_title").text();
        final String supplierName = document.select(".sprite-ico-rocket-w").text();
        final String supplyingCity = document.select("#current-city").text();
        final String supplyAvgTime = document.select(".sprite-ico-timer-2").text();

        return Observable.create(subscriber -> {
            try {
                for (Element product : document.select(".product-item")) {
                    Optional<BigDecimal> cost = Optional.of(product.select(".product-item_bonus span"))
                            .map(Elements::first)
                            .map(Element::text)
                            .map(this::parsePrice);

                    if (!cost.isPresent()) {
                        continue;
                    }

                    Food food = new Food(
                            restaurantName,
                            supplierName,
                            supplyingCity,
                            Collections.emptyList(),
                            BigDecimal.valueOf(0),
                            null,
                            null,
                            product.select(".product-item_title").text(),
                            null,
                            cost.get(),
                            0.0,
                            product.select(".ingredients p").text(),
                            product.select(".product-item_image img").attr("src"),
                            Collections.emptyList()
                    );

                    subscriber.onNext(food);
                }

                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

    private BigDecimal parsePrice(String price) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(price));
        } catch (NumberFormatException e) {
            LOG.error("", e);
            return BigDecimal.valueOf(0.0);
        }
    }

    public static void main(String[] args) throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        new OllisExtractor().extract()
                .doOnNext(e -> count.incrementAndGet())
                .map(food -> food.getTitle())
                .subscribe(food1 -> LOG.info("{}", food1), e -> LOG.error("", e), () -> LOG.info("count: {}", count));


        TimeUnit.HOURS.sleep(2);
    }
}
