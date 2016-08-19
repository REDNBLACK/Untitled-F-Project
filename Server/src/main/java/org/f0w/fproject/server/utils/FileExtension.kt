package org.f0w.fproject.server.utils;

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths

fun File.streamFromResources() = ClassLoader.getSystemClassLoader().getResourceAsStream(this.path)

fun File.pointToResources() = File(ClassLoader.getSystemClassLoader().getResource(this.path).toURI())

fun File.toStringFromResources(): String {
    try {
        return String(
            Files.readAllBytes(
                Paths.get(ClassLoader.getSystemClassLoader().getResource(this.path).toURI())
            )
        )
    } catch (e: NullPointerException) {
        throw DomainException(e)
    } catch (e: IOException) {
        throw DomainException(e)
    } catch (e: URISyntaxException) {
        throw DomainException(e)
    }
}