package org.f0w.fproject.server.service.extractor;

public class ExtractionException extends RuntimeException {
    public ExtractionException() {
        super();
    }

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtractionException(Throwable cause) {
        super(cause);
    }

    protected ExtractionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
