package fr.leowenex.hashtransfer.dto;

public record FileUploadResponse (
        String fileId,
        String filename,
        String message
) {}
