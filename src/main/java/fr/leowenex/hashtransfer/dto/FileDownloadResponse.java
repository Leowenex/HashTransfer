package fr.leowenex.hashtransfer.dto;

import org.springframework.core.io.Resource;

public record FileDownloadResponse(
        FileData fileData,
        Resource fileResource
) {}

