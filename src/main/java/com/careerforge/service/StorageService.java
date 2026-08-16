package com.careerforge.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {

    /**
     * Stores a file on the underlying storage and returns the safe stored file name.
     *
     * @param file the multipart file to store
     * @return the generated stored file name (e.g. UUID-based)
     */
    String store(MultipartFile file);

    /**
     * Loads a stored file as a Spring Resource for download / streaming.
     *
     * @param storedFileName the unique filename under storage
     * @return Resource representing the file
     */
    Resource loadAsResource(String storedFileName);

    /**
     * Deletes a stored file from the storage system.
     *
     * @param storedFileName the unique filename under storage
     */
    void delete(String storedFileName);

    /**
     * Checks if a file exists in the storage system.
     *
     * @param storedFileName the unique filename under storage
     * @return true if the file exists, false otherwise
     */
    boolean exists(String storedFileName);

    /**
     * Resolves the absolute path for a stored file.
     *
     * @param storedFileName the unique filename under storage
     * @return Path representing the absolute path on filesystem
     */
    Path getFilePath(String storedFileName);
}
