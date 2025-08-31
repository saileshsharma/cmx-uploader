-- Create table if missing (matches your Java entity with fnol_reference_no)
CREATE TABLE IF NOT EXISTS image_asset (
    id BIGSERIAL PRIMARY KEY,
    fnol_reference_no TEXT NOT NULL,
    kind VARCHAR(50) NOT NULL,
    filename TEXT NOT NULL,
    ext TEXT,
    mime_type TEXT,
    size_bytes BIGINT NOT NULL,
    width_px INT,
    height_px INT,
    checksum_sha256 CHAR(64) NOT NULL,
    gcs_bucket TEXT NOT NULL,
    gcs_object VARCHAR(512) NOT NULL,
    storage_uri VARCHAR(1024) NOT NULL,
    exif_json JSONB,
    uploaded_by TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL
);

-- Drop the old unique constraint if it exists (name may vary)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'image_asset_fnol_id_checksum_kind_key') THEN
    ALTER TABLE image_asset DROP CONSTRAINT image_asset_fnol_id_checksum_kind_key;
  END IF;
END$$;

-- Create the new unique constraint if missing
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'image_asset_fnolref_checksum_kind_key') THEN
    ALTER TABLE image_asset
      ADD CONSTRAINT image_asset_fnolref_checksum_kind_key
      UNIQUE (fnol_reference_no, checksum_sha256, kind);
  END IF;
END$$;

-- Helpful indexes (idempotent)
CREATE INDEX IF NOT EXISTS idx_image_asset_fnolref   ON image_asset(fnol_reference_no);
CREATE INDEX IF NOT EXISTS idx_image_asset_checksum  ON image_asset(checksum_sha256);
CREATE INDEX IF NOT EXISTS idx_image_asset_kind      ON image_asset(kind);
