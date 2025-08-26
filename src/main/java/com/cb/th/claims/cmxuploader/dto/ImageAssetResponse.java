package com.cb.th.claims.cmxuploader.dto;

import com.cb.th.claims.cmxuploader.domain.ImageAsset;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageAssetResponse {
    private Long id;
    private String kind;
    private String filename;
    private String mimeType;
    private long sizeBytes;
    private Integer widthPx;
    private Integer heightPx;
    private String storageUri;
    private OffsetDateTime uploadedAt;

    public static ImageAssetResponse from(ImageAsset a) {
        return ImageAssetResponse.builder().id(a.getId()).kind(a.getKind().name()).filename(a.getFilename()).mimeType(a.getMimeType()).sizeBytes(a.getSizeBytes()).widthPx(a.getWidthPx()).heightPx(a.getHeightPx()).storageUri(a.getStorageUri()).uploadedAt(a.getUploadedAt()).build();
    }
}
