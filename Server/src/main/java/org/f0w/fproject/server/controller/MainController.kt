package org.f0w.fproject.server.controller

import org.springframework.boot.autoconfigure.web.ErrorController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse
import java.io.IOException

@RestController
class MainController : ErrorController {
    @RequestMapping("/error")
    fun error() = "Ошибка приложения"

    override fun getErrorPath() = "/error"

    @RequestMapping("/")
    @Throws(IOException::class)
    fun index(response: HttpServletResponse) {
        response.sendRedirect("/info")
    }
}
