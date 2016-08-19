package org.f0w.fproject.server.service.extractor;

import org.f0w.fproject.server.domain.Food;
import rx.Observable;

public interface Extractor {
    Observable<Food> extract();
}
