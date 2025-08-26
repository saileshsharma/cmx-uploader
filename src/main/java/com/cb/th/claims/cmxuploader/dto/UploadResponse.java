package com.cb.th.claims.cmxuploader.dto;

import com.cb.th.claims.cmxuploader.domain.ImageAsset;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadResponse {
    private Long id;
    private String storageUri;
    private String mimeType;
    private long sizeBytes;
    private Integer widthPx;
    private Integer heightPx;
    private String description;

    // Alias "url" -> storageUri for frontend convenience
    @JsonProperty("url")
    public String getUrl() {
        return storageUri;
    }

    public static UploadResponse from(ImageAsset a) {
        return UploadResponse.builder()
                .id(a.getId())
                .storageUri(a.getStorageUri())
                .mimeType(a.getMimeType())
                .sizeBytes(a.getSizeBytes())
                .widthPx(a.getWidthPx())
                .heightPx(a.getHeightPx())
                .description(a.getDescription())
                .build();
    }
}