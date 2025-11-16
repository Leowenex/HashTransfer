package fr.leowenex.hashtransfer.service;

import fr.leowenex.hashtransfer.exception.DataAccessException;
import fr.leowenex.hashtransfer.exception.InvalidFilePathException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalStorageFileUtils {

    /**
     * Get the absolute normalized path of the file storage directory.
     * If the directory does not exist, it is created.
     * @param fileStorageDirectory the file storage directory path as a string
     * @return the absolute normalized path of the file storage directory
     */
    public static Path GetFileStorageDirectoryPath(String fileStorageDirectory) {
        Path fileStorageDirectoryPath = Paths.get(fileStorageDirectory).toAbsolutePath().normalize();
        if (!fileStorageDirectoryPath.toFile().exists()) {
            if (!fileStorageDirectoryPath.toFile().mkdirs()) {
                throw new DataAccessException("Could not create file storage directory");
            }
        }
        return fileStorageDirectoryPath;
    }

    /**
     * Get file directory path for a given file id.
     * Performs path traversal validation.
     * @param fileStorageDirectoryPath the path to the file storage directory
     * @param fileId the file id
     * @return the path to the file directory
     */
    public static Path GetFileDirectoryPath(Path fileStorageDirectoryPath, String fileId) {
        Path fileDirectoryPath = fileStorageDirectoryPath.resolve(fileId).normalize();
        if (!fileDirectoryPath.startsWith(fileStorageDirectoryPath)) {
            throw new InvalidFilePathException("File id cannot contain path traversal sequences");
        }
        return fileDirectoryPath;
    }

    /**
     * Get the absolute normalized path of an uploaded file.
     * Performs path traversal validation.
     * @param uploadedFileDirectoryPath the path to the uploaded file directory
     * @param fileName the name of the uploaded file
     * @return the absolute normalized path of the uploaded file
     */
    public static Path GetUploadedFilePath(Path uploadedFileDirectoryPath, String fileName) {
        Path uploadedFilePath = uploadedFileDirectoryPath.resolve(fileName).normalize();
        if (!uploadedFilePath.startsWith(uploadedFileDirectoryPath)) {
            throw new InvalidFilePathException("File name cannot contain path traversal sequences");
        }
        return uploadedFilePath;
    }
}
