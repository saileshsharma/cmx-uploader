package com.cb.th.claims.cmxuploader.controller;

import com.cb.th.claims.cmxuploader.cenum.ImageKind;
import com.cb.th.claims.cmxuploader.domain.ImageAsset;
import com.cb.th.claims.cmxuploader.dto.UploadResponse;
import com.cb.th.claims.cmxuploader.repo.ImageAssetRepository;
import com.cb.th.claims.cmxuploader.service.ImageMetadataService;
import com.cb.th.claims.cmxuploader.service.ImageStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" }, // adjust for your UI
    allowedHeaders = { "*" }, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS })
@RequestMapping("/api/fnol/{fnolRef}/images")
public class FnolImageController {

  private static final Pattern ILLEGAL_FS_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

  private final ImageStorageService storageService;
  private final ImageMetadataService meta;
  private final ImageAssetRepository repo;

  public FnolImageController(ImageStorageService storageService,
      ImageMetadataService meta,
      ImageAssetRepository repo) {
    this.storageService = storageService;
    this.meta = meta;
    this.repo = repo;
  }

  /**
   * Upload a single image (bucketed by fnolRef). Accepts optional "description".
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  public ResponseEntity<UploadResponse> upload(@PathVariable("fnolRef") String fnolRef,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "description", required = false) String description)
      throws IOException {

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
    }

    // ---- Sanitize inputs ----
    final String safeFnolRef = sanitizeSegment(fnolRef);
    if (safeFnolRef.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fnolRef");

    final String originalName = StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
    final String baseName = Paths.get(originalName).getFileName().toString();
    final String safeName = sanitizeSegment(baseName).isBlank() ? "file" : sanitizeSegment(baseName);

    final String mime = file.getContentType() != null
        ? file.getContentType()
        : MediaType.APPLICATION_OCTET_STREAM_VALUE;

    final String safeDesc = sanitizeDescription(description, 256);

    // ---- Buffer once (avoid multiple stream opens) ----
    final byte[] bytes = file.getBytes();

    final String checksum = meta.sha256(new ByteArrayInputStream(bytes));
    final var basic = meta.probeImage(new ByteArrayInputStream(bytes), mime);
    final String exifJson = meta.extractExifJson(new ByteArrayInputStream(bytes));

    // ---- Duplicate check by (fnolRef, checksum, kind) ----
    if (repo.existsByFnolReferenceNoAndChecksumSha256AndKind(safeFnolRef, checksum, ImageKind.ORIGINAL)) {
      final URI existing = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/fnol/{fnolRef}/images/{fileName}")
          .buildAndExpand(safeFnolRef, safeName)
          .toUri();

      // Build a minimal DTO to mirror what the FE expects
      UploadResponse dup = UploadResponse.builder()
          .id(null)
          .storageUri(existing.toString())
          .mimeType(mime)
          .sizeBytes((long) bytes.length)
          .widthPx(basic.width())
          .heightPx(basic.height())
          .description(safeDesc) // echo request desc; DB unchanged
          .build();

      return ResponseEntity.status(208).body(dup);
    }

    // ---- Store under <baseDir>/<fnolRef>/<safeName> ----
    final String localPath = storageService.uploadBytes(safeFnolRef, safeName, bytes, mime);

    final URI fileUri = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/fnol/{fnolRef}/images/{fileName}")
        .buildAndExpand(safeFnolRef, safeName)
        .toUri();

    ImageAsset asset = ImageAsset.builder()
        .fnolReferenceNo(safeFnolRef)
        .kind(ImageKind.ORIGINAL)
        .filename(safeName)
        .ext(extOf(safeName))
        .mimeType(mime)
        .sizeBytes((long) bytes.length)
        .widthPx(basic.width())
        .heightPx(basic.height())
        .checksumSha256(checksum)
        .gcsBucket("local")
        .gcsObject(safeFnolRef + "/" + safeName)
        .storageUri(fileUri.toString())
        .exifJson(exifJson)
        .uploadedBy("system")
        .uploadedAt(OffsetDateTime.now())
        .description(safeDesc)
        .build();

    asset = repo.save(asset);

    // If you want to expose localPath too, add it to the DTO
    UploadResponse body = UploadResponse.from(asset);
    return ResponseEntity.created(fileUri).body(body);
  }

  /** Download raw bytes by file name under the given fnolRef folder. */
  @GetMapping(path = "/{fileName}")
  public ResponseEntity<Resource> download(@PathVariable("fnolRef") String fnolRef,
      @PathVariable String fileName) throws IOException {
    final String safeFnolRef = sanitizeSegment(fnolRef);
    if (safeFnolRef.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fnolRef");
    final String safeName = Paths.get(fileName).getFileName().toString();
    if (safeName.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");

    byte[] data = storageService.download(safeFnolRef, safeName);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .contentType(contentTypeFor(safeName))
        .body(new ByteArrayResource(data));
  }

  // ===== Helpers =====

  private static String sanitizeSegment(String s) {
    String v = Objects.toString(s, "").trim();
    if (v.startsWith(":"))
      v = v.substring(1);
    v = ILLEGAL_FS_CHARS.matcher(v).replaceAll("_");
    v = v.replaceAll("^_+|_+$", "");
    return v;
  }

  /** Trim, remove control chars, collapse spaces, and clamp length. */
  private static String sanitizeDescription(String s, int max) {
    if (s == null)
      return null;
    String t = s.trim();
    t = t.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", " ").replaceAll("\\s+", " ");
    if (t.length() > max)
      t = t.substring(0, max);
    return t.isBlank() ? null : t;
  }

  private static MediaType contentTypeFor(String fileName) {
    String n = fileName.toLowerCase();
    if (n.endsWith(".png"))
      return MediaType.IMAGE_PNG;
    if (n.endsWith(".jpg") || n.endsWith(".jpeg"))
      return MediaType.IMAGE_JPEG;
    if (n.endsWith(".webp"))
      return MediaType.parseMediaType("image/webp");
    return MediaType.APPLICATION_OCTET_STREAM;
  }

  private static String extOf(String name) {
    int dot = name.lastIndexOf('.');
    return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
  }
}