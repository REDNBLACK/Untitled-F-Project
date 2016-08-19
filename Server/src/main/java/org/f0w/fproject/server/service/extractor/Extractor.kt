package org.f0w.fproject.server.service.extractor

import org.f0w.fproject.server.domain.Food
import rx.Observable

interface Extractor {
    fun extract(): Observable<Food>
}
