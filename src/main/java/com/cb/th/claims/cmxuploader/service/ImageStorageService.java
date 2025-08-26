package com.cb.th.claims.cmxuploader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
public class ImageStorageService {

  @Value("${uploader.base-dir:./uploader-data}")
  private String baseDir;

  public String uploadBytes(String fnolRef, String fileName, byte[] bytes, String mime) throws IOException {
    Path base = Paths.get(baseDir).toAbsolutePath().normalize();
    Path dir  = base.resolve(fnolRef).normalize();
    if (!dir.startsWith(base)) throw new IllegalArgumentException("Invalid path");
    Files.createDirectories(dir);

    Path dest = dir.resolve(fileName);
    Files.write(dest, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return dest.toString();
  }

  public byte[] download(String fnolRef, String fileName) throws IOException {
    Path base = Paths.get(baseDir).toAbsolutePath().normalize();
    Path path = base.resolve(fnolRef).resolve(fileName).normalize();
    if (!path.startsWith(base)) throw new IllegalArgumentException("Invalid path");
    return Files.readAllBytes(path);
  }
}
