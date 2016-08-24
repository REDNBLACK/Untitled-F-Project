package org.f0w.fproject.server.utils;

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths

fun File.streamFromResources(): InputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(this.path)

fun File.pointToResources() = File(ClassLoader.getSystemClassLoader().getResource(this.path).toURI())

fun File.toStringFromResources(): String {
    try {
        return String(
            Files.readAllBytes(
                Paths.get(ClassLoader.getSystemClassLoader().getResource(this.path).toURI())
            )
        )
    } catch (e: Exception) {
        when (e) {
            is DomainException, is IOException, is URISyntaxException -> throw DomainException(e)
            else -> throw e
        }
    }
}