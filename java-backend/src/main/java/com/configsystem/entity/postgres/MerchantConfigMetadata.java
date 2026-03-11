package com.configsystem.entity.postgres;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps to the existing "json_metadata" table.
 *
 * This table stores dot-notation JSON paths and their inferred data types
 * extracted from all merchant configurations.
 */
@Entity
@Table(name = "json_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantConfigMetadata {

    /** Dot-notation JSON path, e.g. "payment_config.interest_rate". Primary key. */
    @Id
    @Column(name = "json_path", nullable = false, unique = true, length = 1024)
    private String jsonPath;

    /** Inferred data type: STRING | NUMBER | BOOLEAN | NULL */
    @Column(name = "data_type", length = 32)
    private String dataType;
}
