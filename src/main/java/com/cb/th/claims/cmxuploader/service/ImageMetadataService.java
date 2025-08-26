package com.cb.th.claims.cmxuploader.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;

@Service
public class ImageMetadataService {

    public record BasicMeta(String mime, int width, int height) {}

    public String sha256(InputStream in) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            in.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), md));
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public BasicMeta probeImage(InputStream in, String fallbackMime) throws IOException {
        BufferedImage img = ImageIO.read(in);
        if (img == null) return new BasicMeta(fallbackMime, 0, 0);
        return new BasicMeta(fallbackMime, img.getWidth(), img.getHeight());
    }

    public String extractExifJson(InputStream in) {
        try {
            var metadata = ImageMetadataReader.readMetadata(in);
            var map = new LinkedHashMap<String, Object>();
            for (Directory dir : metadata.getDirectories()) {
                var d = new LinkedHashMap<String, Object>();
                for (Tag t : dir.getTags()) {
                    d.put(t.getTagName(), t.getDescription());
                }
                map.put(dir.getName(), d);
            }
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return null; // swallow EXIF errors
        }
    }
}
