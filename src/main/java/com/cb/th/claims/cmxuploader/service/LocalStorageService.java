package com.cb.th.claims.cmxuploader.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class LocalStorageService {

    private final Path root;

    public LocalStorageService(@Value("${cmx.uploader.storage-dir}") String storageDir) throws IOException {
        this.root = Paths.get(storageDir);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
    }

    public String saveFile(MultipartFile file, String fnolId) throws IOException {
        Path fnolDir = root.resolve(fnolId);
        if (!Files.exists(fnolDir)) {
            Files.createDirectories(fnolDir);
        }
        Path target = fnolDir.resolve(file.getOriginalFilename());
        file.transferTo(target.toFile());
        return target.toAbsolutePath().toString();
    }

    public byte[] getFile(String fnolId, String fileName) throws IOException {
        Path file = root.resolve(fnolId).resolve(fileName);
        return Files.readAllBytes(file);
    }
}
