package org.f0w.fproject.server.service.extractor.zakazaka;

import org.f0w.fproject.server.service.extractor.AbstractExtractor;

abstract class BaseZakaZakaExtractor extends AbstractExtractor {
    private final String url;

    public BaseZakaZakaExtractor(final String url) {
        this.url = url;
    }
}
