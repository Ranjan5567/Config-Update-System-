package com.configsystem.repository.postgres;

import com.configsystem.entity.postgres.MerchantConfigMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the global json_metadata table.
 *
 * PK is String (json_path), so save() calls merge() — acts as upsert.
 * deleteAllInBatch() (inherited) used by seedAll() for full wipe + re-seed.
 * existsById() (inherited) used by MerchantConfigService to validate update paths.
 */
@Repository
public interface MerchantConfigMetadataRepository extends JpaRepository<MerchantConfigMetadata, String> {
}
