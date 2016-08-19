package org.f0w.fproject.server.service.extractor.zakazaka

import com.squareup.okhttp.OkHttpClient
import org.f0w.fproject.server.service.cuisine.StaticCuisineDetectionStrategy
import org.yaml.snakeyaml.Yaml
import org.f0w.fproject.server.utils.streamFromResources
import java.io.File

class OllisExtractor(client: OkHttpClient) : BaseZakaZakaExtractor(
        ZakaZakaConfig(
                "ollis",
                Yaml().load(File("areas/Saint-Petersburg.yml").streamFromResources()) as List<String>
        ),
        StaticCuisineDetectionStrategy("Мультинациональная кухня"),
        client
) {}
