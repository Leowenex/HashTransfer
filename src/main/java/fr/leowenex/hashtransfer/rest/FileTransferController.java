package fr.leowenex.hashtransfer.rest;

import fr.leowenex.hashtransfer.dto.FileData;
import fr.leowenex.hashtransfer.dto.FileDownloadResponse;
import fr.leowenex.hashtransfer.dto.FileUploadResponse;
import fr.leowenex.hashtransfer.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileTransferController {

    private final FileService fileService;

    @GetMapping("/{fileId}/download")
    public ResponseEntity<@NonNull Resource> downloadFile(@PathVariable String fileId, @RequestParam(required = false, name = "dib") boolean displayInBrowser) throws IOException {

        FileDownloadResponse downloadResponse = fileService.downloadFile(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        Resource fileResource = downloadResponse.fileResource();
        FileData fileData = downloadResponse.fileData();

        MediaType mediaType = resolveMediaType(fileData);

        HttpHeaders headers = new HttpHeaders();
        ContentDisposition contentDisposition = displayInBrowser
                ? ContentDisposition.inline().filename(fileData.filename()).build()
                : ContentDisposition.attachment().filename(fileData.filename()).build();
        headers.setContentDisposition(contentDisposition);
        headers.setContentLength(fileResource.contentLength());
        headers.setContentType(mediaType);
        headers.add("Content-Digest", "sha-256=" + fileData.sha256());

        return ResponseEntity.ok().headers(headers).body(fileResource);
    }

    private MediaType resolveMediaType(FileData fileData) {
        if (fileData.contentType() != null) {
            try {
                return MediaType.parseMediaType(fileData.contentType());
            } catch (Exception _) {
            }
        }
        return resolveMediaType(fileData.filename());
    }

    private MediaType resolveMediaType(String filename) {
        try {
            String contentType = Files.probeContentType(Paths.get(filename));
            if (contentType != null) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException _) {
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<@NonNull FileData> getFileData(@PathVariable String fileId) {
        FileData fileData = fileService.queryFileData(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        return ResponseEntity.ok(fileData);
    }


    @PostMapping
    public ResponseEntity<@NonNull FileUploadResponse> uploadFile(@RequestPart(value = "file") MultipartFile file, @RequestPart(value = "sha256", required = false) String sha256) throws IOException {

        if (ObjectUtils.isEmpty(file.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must have a filename");
        }

        FileUploadResponse uploadResponse;

        try (InputStream inputStream = file.getInputStream()) {
            uploadResponse = fileService.uploadFile(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    sha256,
                    inputStream
            );
        }


        return ResponseEntity.ok(uploadResponse);
    }




}
