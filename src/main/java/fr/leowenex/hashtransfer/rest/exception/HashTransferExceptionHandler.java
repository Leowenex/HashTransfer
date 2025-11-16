package fr.leowenex.hashtransfer.rest.exception;

import fr.leowenex.hashtransfer.exception.DataAccessException;
import fr.leowenex.hashtransfer.exception.DigestNotMatchingException;
import fr.leowenex.hashtransfer.exception.InvalidFilePathException;
import fr.leowenex.hashtransfer.exception.UnreadableMetadataException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class HashTransferExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<@NonNull ErrorObject> handleFileAccessException(DataAccessException ex, WebRequest request) {
        return makeResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(DigestNotMatchingException.class)
    public ResponseEntity<@NonNull ErrorObject> handleDigestNotMatchingException(DigestNotMatchingException ex, WebRequest request) {
        return makeResponse(HttpStatus.EXPECTATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidFilePathException.class)
    public ResponseEntity<@NonNull ErrorObject> handleInvalidFilePathException(InvalidFilePathException ex, WebRequest request) {
        return makeResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NonNull ErrorObject> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return makeResponse(HttpStatus.BAD_REQUEST, "Invalid argument for parameter: " + ex.getName(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<@NonNull ErrorObject> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        return makeResponse((HttpStatus) ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : "Error", request);
    }

    @ExceptionHandler(UnreadableMetadataException.class)
    public ResponseEntity<@NonNull ErrorObject> handleUnreadableMetadataException(UnreadableMetadataException ex, WebRequest request) {
        return makeResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unreadable file metadata: " + ex.getMessage(), request);
    }

    private static ResponseEntity<@NonNull ErrorObject> makeResponse(HttpStatus status, String error, WebRequest request) {
        ErrorObject errorObject = ErrorObject.of(status, error, request);
        return ResponseEntity.status(status).body(errorObject);
    }
}
