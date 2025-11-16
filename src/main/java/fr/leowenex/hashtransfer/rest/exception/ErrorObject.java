package fr.leowenex.hashtransfer.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.ZonedDateTime;

public record ErrorObject (
        ZonedDateTime timestamp,
        int status,
        String error,
        String path
) {

    public static ErrorObject of(HttpStatus status, String error, WebRequest request) {
        ZonedDateTime timestamp = ZonedDateTime.now();
        int statusCode = status.value();
        String path = ((ServletWebRequest)request).getRequest().getRequestURI();
        return new ErrorObject(timestamp, statusCode, error, path);
    }

}
