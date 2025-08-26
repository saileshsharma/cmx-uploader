ALTER TABLE image_asset
  ALTER COLUMN checksum_sha256 TYPE VARCHAR(64)
  USING checksum_sha256::varchar(64);