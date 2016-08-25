package org.f0w.fproject.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import mu.KLogging
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import rx.Observable
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicBoolean

@Component
class ElasticToCSVExporter(
        @Autowired private val elastic: Client,
        @Autowired private val jsonMapper: ObjectMapper,
        @Autowired private val csvMapper: CsvMapper
) {
    companion object: KLogging()

    private fun getRaw(index: String): Observable<ByteArray> {
        return Observable.create<ByteArray> { subscriber ->
            try {
                val timeValue = TimeValue(6000)

                var scrollResp = elastic.prepareSearch(index)
                        .setScroll(timeValue)
                        .setSize(100)
                        .execute()
                        .actionGet()

                while (true) {
                    for (hit in scrollResp.hits.hits) {
                        subscriber.onNext(hit.source())
                    }

                    scrollResp = elastic.prepareSearchScroll(scrollResp.scrollId)
                            .setScroll(timeValue)
                            .execute()
                            .actionGet()

                    if (scrollResp.hits.hits.size == 0) {
                        break
                    }
                }
            } catch (e: Exception) {
                subscriber.onError(e)
            }

            subscriber.onCompleted()
        }
    }

    fun <T> exportToFile(file: File, index: String, clazz: Class<T>) {
        BufferedWriter(FileWriter(file, true)).use { fileWriter ->
            val isFirst = AtomicBoolean(true)
            val csvWriter = csvMapper.writerWithSchemaFor(clazz)

            getRaw(index)
                    .map { jsonMapper.readValue(it, clazz) }
                    .map {
                        if (isFirst.get()) {
                            csvMapper.writer(csvMapper.schemaFor(clazz).withHeader())
                                    .writeValueAsString(it)
                        } else {
                            csvWriter.writeValueAsString(it)
                        }
                    }
                    .doOnNext { isFirst.set(false) }
                    .subscribe(
                            { fileWriter.write(it) },
                            { logger.error { it } },
                            { logger.info { "Запись в CSV файл $file, успешно завершена." } }
                    )
        }
    }
}