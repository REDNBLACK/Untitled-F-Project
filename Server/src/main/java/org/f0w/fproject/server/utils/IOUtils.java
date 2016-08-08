package org.f0w.fproject.server.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class IOUtils {
    private IOUtils() {}

    public static String readFileFromResources(final String path) {
        try {
            return new String(
                Files.readAllBytes(Paths.get(IOUtils.class.getClassLoader().getResource(path).toURI())),
                Charset.defaultCharset()
            );
        } catch (NullPointerException | IOException | URISyntaxException e) {
            throw new DomainException(e);
        }
    }
}
