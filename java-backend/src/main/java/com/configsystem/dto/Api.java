package com.configsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API request/response types.
 */
public final class Api {

    private Api() {}

    // --- API 1: Get Current Attribute Value ---
    public record AttributeValueRequest(
            @NotNull Long merchantId,
            @NotNull String attribute
    ) {}

    public record AttributeValueResponse(
            boolean success,
            String attribute,
            Object value,
            String message
    ) {
        public static AttributeValueResponse ok(String attribute, Object value) {
            return new AttributeValueResponse(true, attribute, value, "Success");
        }
        public static AttributeValueResponse error(String message) {
            return new AttributeValueResponse(false, null, null, message);
        }
    }

    // --- API 2: Update DB with new value ---
    public record UpdateValueRequest(
            @NotNull String createdBy,
            @NotNull Long merchantId,
            @NotNull String attributeChanged,
            String valueFrom,
            @NotNull Object valueTo
    ) {}

    public record GenericResponse(
            boolean success,
            String message
    ) {
        public static GenericResponse ok(String message) {
            return new GenericResponse(true, message);
        }
        public static GenericResponse error(String message) {
            return new GenericResponse(false, message);
        }
    }

    // --- API 3: Retrieve All Merchants Details ---
    public record MerchantDetail(
            Long id,
            String name
    ) {}

    // --- API 4: Store Audit Logs ---
    public record StoreAuditLogRequest(
            @NotNull String createdBy,
            @NotNull Long merchantId,
            @NotNull String attributeChanged,
            String valueFrom,
            String valueTo
    ) {}

    // --- API 5: Retrieve Audit Logs ---
    public record AuditLogQueryRequest(
            @NotNull String query
    ) {}
}
