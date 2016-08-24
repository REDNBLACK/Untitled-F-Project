package org.f0w.fproject.server.service.extractor

import com.squareup.okhttp.OkHttpClient
import org.elasticsearch.client.Client
import org.f0w.fproject.server.service.extractor.zakazaka.ZakaZakaExtractorFactory
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml

@Component
open class ExtractorsFactory(
        private val yaml: Yaml,
        private val elastic: Client,
        private val httpClient: OkHttpClient
) {
    open fun getZakaZakaExtractorFactory(): ExtractorFactory {
        return ZakaZakaExtractorFactory(yaml, elastic, httpClient)
    }
}
