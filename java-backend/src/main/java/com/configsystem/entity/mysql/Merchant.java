package com.configsystem.entity.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Maps to the existing "merchants" table in MySQL.
 *
 * Columns:  merchant_id (PK), config_json (JSON)
 *
 * DDL_AUTO is set to "update" in MysqlConfig — Hibernate will
 * create or alter the table schema to match this entity on startup.
 */
@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @Column(name = "merchant_id")
    private Long merchantId;

    /**
     * The merchant's full configuration as JSON in MySQL.
     * Hibernate 6 maps MySQL's native JSON column to a String automatically.
     */
    @Column(name = "config_json", columnDefinition = "json")
    private String configJson;

    // created_at / updated_at intentionally omitted — not required by the
    // application logic and not guaranteed to exist on every deployment.
}
