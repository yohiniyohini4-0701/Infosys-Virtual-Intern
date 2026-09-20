package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.exception.BadRequestException;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path storageLocation;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".pdf", ".jpg", ".jpeg", ".png"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public DocumentStorageService(@Value("${app.upload.dir:uploads/documents}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize document storage folder at: " + uploadDir, ex);
        }
    }

    public record StoredDocumentInfo(
            String originalFileName,
            String storedFileName,
            String contentType,
            long sizeBytes,
            String storagePath
    ) {}

    public StoredDocumentInfo storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store empty document");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size (" + file.getSize() + " bytes) exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file format [" + contentType + "]. Only PDF, JPG, and PNG are permitted");
        }

        String rawOriginalFilename = file.getOriginalFilename();
        String originalFilename = StringUtils.cleanPath(rawOriginalFilename != null ? rawOriginalFilename : "document.pdf");

        // Prevent path traversal
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Validate extension
        String lowerName = originalFilename.toLowerCase();
        boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!validExt) {
            throw new BadRequestException("File extension is not allowed. Permitted extensions: .pdf, .jpg, .jpeg, .png");
        }

        // Generate safe unique filename to avoid collision and traversal
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }
        String safeBaseName = originalFilename.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String uniqueStoredName = UUID.randomUUID() + "_" + safeBaseName;

        try {
            Path targetPath = this.storageLocation.resolve(uniqueStoredName).normalize();
            if (!targetPath.startsWith(this.storageLocation)) {
                throw new BadRequestException("Path traversal attempt detected");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return new StoredDocumentInfo(
                    originalFilename,
                    uniqueStoredName,
                    contentType,
                    file.getSize(),
                    targetPath.toString()
            );
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file: " + originalFilename, ex);
        }
    }

    public Resource loadFileAsResource(String storagePathOrName) {
        try {
            Path filePath = Paths.get(storagePathOrName).normalize();
            if (!filePath.isAbsolute()) {
                filePath = this.storageLocation.resolve(filePath).normalize();
            }

            if (!Files.exists(filePath)) {
                // If it's a demo path, create dummy demo document content on the fly if needed
                if (filePath.toString().contains("demo_")) {
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, "%PDF-1.4 [Government Verified Document - Simulated Sandbox Content]");
                } else {
                    throw new ResourceNotFoundException("Document file not found at: " + storagePathOrName);
                }
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Document not readable: " + storagePathOrName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Document path invalid: " + storagePathOrName);
        } catch (IOException ex) {
            throw new RuntimeException("Error accessing file: " + storagePathOrName, ex);
        }
    }
}
