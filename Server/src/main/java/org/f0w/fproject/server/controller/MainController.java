package org.f0w.fproject.server.controller;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class MainController implements ErrorController {
    @RequestMapping("/error")
    public String error() {
        return "Ошибка приложения";
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping("/")
    public void index(final HttpServletResponse response) throws IOException {
        response.sendRedirect("/info");
    }
}
