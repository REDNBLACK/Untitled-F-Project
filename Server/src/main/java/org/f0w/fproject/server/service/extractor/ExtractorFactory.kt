package org.f0w.fproject.server.service.extractor

interface ExtractorFactory {
    fun make(restaurant: String): Extractor

    fun getName(): String
}
