package com.careerforge.service.impl;

import com.careerforge.config.StorageConfigProperties;
import com.careerforge.exception.FileStorageException;
import com.careerforge.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation;
    private final StorageConfigProperties storageConfig;

    public LocalStorageServiceImpl(StorageConfigProperties storageConfig) {
        this.storageConfig = storageConfig;
        this.rootLocation = Paths.get(storageConfig.getLocalDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Initialized local file storage directory at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory: " + rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file");
        }

        String rawOriginalFilename = file.getOriginalFilename();
        String originalFilename = rawOriginalFilename != null ? StringUtils.cleanPath(rawOriginalFilename) : "resume.pdf";

        // Security check: Check for path traversal in original filename
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Generate safe unique stored filename
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!".pdf".equals(fileExtension)) {
            fileExtension = ".pdf";
        }

        String storedFileName = UUID.randomUUID() + fileExtension;
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize().toAbsolutePath();

        // Security check: Verify destination stays within storage root
        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new FileStorageException("Cannot store file outside current directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file {} as {}", originalFilename, storedFileName);
            return storedFileName;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public Resource loadAsResource(String storedFileName) {
        try {
            Path file = rootLocation.resolve(storedFileName).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Cannot access file outside current directory: " + storedFileName);
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Could not read file: " + storedFileName);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not read file: " + storedFileName, e);
        }
    }

    @Override
    public void delete(String storedFileName) {
        try {
            Path file = rootLocation.resolve(storedFileName).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new FileStorageException("Cannot delete file outside current directory: " + storedFileName);
            }
            Files.deleteIfExists(file);
            log.debug("Deleted file: {}", storedFileName);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storedFileName, e);
        }
    }

    @Override
    public boolean exists(String storedFileName) {
        Path file = rootLocation.resolve(storedFileName).normalize();
        if (!file.startsWith(rootLocation)) {
            return false;
        }
        return Files.exists(file);
    }

    @Override
    public Path getFilePath(String storedFileName) {
        Path file = rootLocation.resolve(storedFileName).normalize();
        if (!file.startsWith(rootLocation)) {
            throw new FileStorageException("Invalid file path resolution: " + storedFileName);
        }
        return file;
    }
}
