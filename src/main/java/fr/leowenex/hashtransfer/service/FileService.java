package fr.leowenex.hashtransfer.service;

import fr.leowenex.hashtransfer.dto.FileData;
import fr.leowenex.hashtransfer.dto.FileDownloadResponse;
import fr.leowenex.hashtransfer.dto.FileUploadResponse;

import java.io.InputStream;
import java.util.Optional;

public interface FileService {

    Optional<FileDownloadResponse> downloadFile(String fileId);
    Optional<FileData> queryFileData(String fileId);
    FileUploadResponse uploadFile(String fileName, long fileSize, String contentType, String sha256, InputStream inputStream);
    void purgeExpiredFiles();
}
