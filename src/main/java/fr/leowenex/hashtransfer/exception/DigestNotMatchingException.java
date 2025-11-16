package fr.leowenex.hashtransfer.exception;

public class DigestNotMatchingException extends RuntimeException {
    public DigestNotMatchingException(String message) {
        super(message);
    }
}
