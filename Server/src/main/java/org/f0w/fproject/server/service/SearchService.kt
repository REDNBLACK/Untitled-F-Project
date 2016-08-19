package org.f0w.fproject.server.service

import org.f0w.fproject.server.domain.SearchRequest
import org.f0w.fproject.server.domain.SearchResponse
import org.springframework.stereotype.Service

@Service
class SearchService {
    fun find(request: SearchRequest): SearchResponse? {
        return null
    }
}
