package fr.leowenex.hashtransfer.dto;

public record FileData (
        String contentType,
        String filename,
        String sha256
) {}
