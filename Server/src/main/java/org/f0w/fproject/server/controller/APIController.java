package org.f0w.fproject.server.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.f0w.fproject.server.domain.SearchRequest;
import org.f0w.fproject.server.domain.SearchResponse;
import org.f0w.fproject.server.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(path = "/api", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class APIController {
    @Autowired
    private SearchService searchService;

    @ApiOperation(value = "Ищет случайные блюда в случайном ресторана не основе параметров запроса")
    @ApiImplicitParams({
        @ApiImplicitParam(
            name = "city",
            value = "Город в котором будет производиться поиск",
            required = true,
            dataType = "string",
            paramType = "query"
        ),
        @ApiImplicitParam(
            name = "area",
            value = "Район города в котором будет производиться поиск",
            required = true,
            dataType = "string",
            paramType = "query"
        ),
        @ApiImplicitParam(
            name = "cuisine",
            value = "Фильтр по типу кухни",
            dataType = "string",
            paramType = "query"
        ),
        @ApiImplicitParam(
            name = "numberOfPersons",
            value = "Количество персон",
            required = true,
            dataType = "string",
            paramType = "query"
        ),
        @ApiImplicitParam(
            name = "cost",
            value = "Максимальная стоимость заказа",
            required = true,
            dataType = "float",
            paramType = "query"
        ),
        @ApiImplicitParam(
            name = "request",
            paramType = "internal"
        )
    })
    @RequestMapping(value = "/find", method = RequestMethod.GET)
    public ResponseEntity<SearchResponse> find(SearchRequest request) {
        return ResponseEntity.ok(searchService.find(request));
    }
}
