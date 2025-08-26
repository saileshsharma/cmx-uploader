package com.cb.th.claims.cmxuploader.domain;

import com.cb.th.claims.cmxuploader.cenum.ImageKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(
  name = "image_asset",
  uniqueConstraints = @UniqueConstraint(
      columnNames = {"fnol_reference_no", "checksum_sha256", "kind"} // << was fnol_id
  )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImageAsset {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fnol_reference_no", nullable = false)
  private String fnolReferenceNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImageKind kind;

  @Column(nullable = false)
  private String filename;

  private String ext;
  private String mimeType;

  @Column(nullable = false)
  private long sizeBytes;

  private Integer widthPx;
  private Integer heightPx;

  @Column(name = "checksum_sha256", length = 64, nullable = false)
  private String checksumSha256;

  @Column(nullable = false)
  private String gcsBucket;

  @Column(nullable = false, length = 512)
  private String gcsObject;

  @Column(nullable = false, length = 1024)
  private String storageUri;


   @JdbcTypeCode(SqlTypes.JSON)          // 👈 tell Hibernate to bind JSON, not VARCHAR
    @Column(name = "exif_json", columnDefinition = "jsonb")
    private String exifJson;              // you can keep this as String (must be valid JSON text)

  private String uploadedBy;

  @Column(nullable = false)
  private OffsetDateTime uploadedAt;


  @Column(length = 256)
  private String description;
}