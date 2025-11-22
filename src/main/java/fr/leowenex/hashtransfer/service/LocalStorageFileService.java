package fr.leowenex.hashtransfer.service;

import fr.leowenex.hashtransfer.config.HashTransferProperties;
import fr.leowenex.hashtransfer.dto.FileData;
import fr.leowenex.hashtransfer.dto.FileDownloadResponse;
import fr.leowenex.hashtransfer.dto.FileUploadResponse;
import fr.leowenex.hashtransfer.exception.DataAccessException;
import fr.leowenex.hashtransfer.exception.DigestNotMatchingException;
import fr.leowenex.hashtransfer.exception.InvalidFilePathException;
import fr.leowenex.hashtransfer.exception.UnreadableMetadataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageFileService implements FileService {

    private final HashTransferProperties hashTransferProperties;

    private final ObjectMapper objectMapper;

    /**
     * Read file metadata from a given path.
     * @param fileMetadataFilePath The path to the metadata file.
     * @return The FileData object containing the metadata.
     */
    private FileData ReadMetadataFromPath(Path fileMetadataFilePath) {
        FileData fileData;
        try {
            fileData = objectMapper.readValue(Files.readString(fileMetadataFilePath), FileData.class);
        } catch (IOException e) {
            log.error("Error reading metadata file: {}", e.getMessage());
            throw new UnreadableMetadataException(e.getMessage());
        }
        return fileData;
    }

    /**
     * Write file metadata to a given path.
     * @param fileMetadataFilePath The path to the metadata file.
     * @param fileData The FileData object containing the metadata to write.
     */
    private void WriteFileMetadataToPath(Path fileMetadataFilePath, FileData fileData) {
        String metadataJson = objectMapper.writeValueAsString(fileData);
        try {
            Files.writeString(fileMetadataFilePath, metadataJson);
        } catch (IOException e) {
            log.error("Error writing metadata file: {}", e.getMessage());
            throw new DataAccessException("Could not write metadata file: " + e.getMessage());
        }
    }

    /**
     * Check the SHA-256 checksum of an uploaded file against the expected checksum.
     * @param computedCheckSum The found SHA-256 checksum.
     * @param expectedSha256CheckSum The expected SHA-256 checksum.
     * @param fileName The name of the file (for logging purposes).
     */
    private static void CheckSha256Checksum(String computedCheckSum, String expectedSha256CheckSum, String fileName) {

        if (!ObjectUtils.isEmpty(expectedSha256CheckSum)){
            if (computedCheckSum.equals(expectedSha256CheckSum)) {
                log.debug("Uploaded file SHA-256 checksum matches the provided checksum for fileName={}", fileName);
            } else {
                log.error("Uploaded file SHA-256 checksum does not match the provided checksum for fileName={}", fileName);
                throw new DigestNotMatchingException("Uploaded file SHA-256 checksum does not match the provided checksum");
            }
        }
    }

    /**
     * Download a file by its ID.
     * @param fileId The ID of the file to download.
     * @return An Optional containing the FileDownloadResponse (File Metadata + File Resource) if the file exists, or empty if not.
     */
    public Optional<FileDownloadResponse> downloadFile(String fileId) {

        log.debug("Received file download request: fileId={}", fileId);

        Path fileStorageDirectoryPath = LocalStorageFileUtils.GetFileStorageDirectoryPath(hashTransferProperties.getFileStorageDirectory());
        Path fileDirectoryPath = LocalStorageFileUtils.GetFileDirectoryPath(fileStorageDirectoryPath, fileId);
        if (!fileDirectoryPath.toFile().exists()) {
            return Optional.empty();
        }

        Path metadataFilePath = fileDirectoryPath.resolve(hashTransferProperties.getMetadataFileName()).normalize();
        FileData fileData = ReadMetadataFromPath(metadataFilePath);
        String filename = fileData.filename();

        Path targetFilePath = fileDirectoryPath.resolve(filename).normalize();
        if (!targetFilePath.toFile().exists()) {
            return Optional.empty();
        }

        Resource fileResource = new FileSystemResource(targetFilePath);
        return Optional.of(new FileDownloadResponse(fileData, fileResource));
    }

    /**
     * Query file metadata by its ID.
     * @param fileId The ID of the file to query.
     * @return An Optional containing the file Metadata if the file exists, or empty if not.
     */
    public Optional<FileData> queryFileData(String fileId) {

        log.debug("Received file data query request: fileId={}", fileId);

        Path fileStorageDirectoryPath = LocalStorageFileUtils.GetFileStorageDirectoryPath(hashTransferProperties.getFileStorageDirectory());
        Path fileDirectoryPath = LocalStorageFileUtils.GetFileDirectoryPath(fileStorageDirectoryPath, fileId);

        if (!fileDirectoryPath.toFile().exists()) {
            return Optional.empty();
        }

        Path metadataFilePath = fileDirectoryPath.resolve(hashTransferProperties.getMetadataFileName()).normalize();

        return Optional.of(ReadMetadataFromPath(metadataFilePath));
    }

    /**
     * Upload a file.
     * @param fileName The original name of the file.
     * @param fileSize The size of the file in bytes.
     * @param contentType The MIME type of the file.
     * @param providedSha256 The SHA-256 checksum of the file provided by caller (optional).
     * @param inputStream The InputStream of the file to upload (to be closed by the caller).
     * @return A FileUploadResponse containing the file ID and status message.
     */
    public FileUploadResponse uploadFile(String fileName, long fileSize, String contentType, String providedSha256, InputStream inputStream) {

        log.debug("Received file upload request: originalFilename={}, size={}, contentType={}", fileName, fileSize, contentType);

        // Check for reserved file name
        if (fileName.equals(hashTransferProperties.getMetadataFileName())) {
            throw new InvalidFilePathException("This file name is reserved and cannot be used");
        }

        Path fileStorageDirectoryPath = LocalStorageFileUtils.GetFileStorageDirectoryPath(hashTransferProperties.getFileStorageDirectory());

        String fileId = UUID.randomUUID().toString();

        Path uploadedFileDirectoryPath = LocalStorageFileUtils.GetFileDirectoryPath(fileStorageDirectoryPath, fileId);
        if (!uploadedFileDirectoryPath.toFile().exists()) {
            if (!uploadedFileDirectoryPath.toFile().mkdirs()) {
                throw new DataAccessException("Could not create target file directory");
            }
        }

        Path uploadedFilePath = LocalStorageFileUtils.GetUploadedFilePath(uploadedFileDirectoryPath, fileName);
        MessageDigest sha256Digest = DigestUtils.getSha256Digest();
        String computedSha256;
        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, sha256Digest); OutputStream outputStream = Files.newOutputStream(uploadedFilePath)) {
            digestInputStream.transferTo(outputStream);
            computedSha256 = HexFormat.of().formatHex(sha256Digest.digest());
            CheckSha256Checksum(computedSha256, providedSha256, fileName);
        } catch (IOException e) {
            log.error("Error writing uploaded file: {}", e.getMessage());
            throw new DataAccessException("Could not write uploaded file: " + e.getMessage());
        }

        Path fileMetadataFilePath = uploadedFileDirectoryPath.resolve(hashTransferProperties.getMetadataFileName()).normalize();
        WriteFileMetadataToPath(fileMetadataFilePath, new FileData(contentType, fileName, computedSha256));

        return new FileUploadResponse(fileId, fileName, "File uploaded successfully");
    }

    /**
     * Purge expired files from the storage directory.
     * Files older than the configured expiration time will be deleted.
     */
    public void purgeExpiredFiles() {
        log.debug("Purging files older than {} minutes", hashTransferProperties.getExpirationMinutes());

        Path fileStorageDirectoryPath = LocalStorageFileUtils.GetFileStorageDirectoryPath(hashTransferProperties.getFileStorageDirectory());

        long currentTime = System.currentTimeMillis();
        long expirationMillis = hashTransferProperties.getExpirationMinutes() * 60L * 1000L;

        try(Stream<Path> stream = Files.walk(fileStorageDirectoryPath)) {

            stream.filter(Files::isDirectory)
                    .filter(path -> !path.equals(fileStorageDirectoryPath))
                    .forEach(path -> {
                        try {
                            long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();

                            if (currentTime - lastModifiedTime > expirationMillis) {
                                log.debug("Purging expired file directory: {}", path);
                                FileUtils.deleteDirectory(path.toFile());
                            }
                        } catch (IOException e) {
                            log.error("Error while checking expiration for directory: {}", path, e);
                        }
                    });

        } catch (IOException e) {
            log.error("Error while purging expired files", e);
        }
    }

}
