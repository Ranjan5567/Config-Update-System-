package com.configsystem.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * API request/response types.
 */
public final class Api {

    private Api() {}

    // ── Requests ──────────────────────────────────────────────────────────────

    public record UpdateRequest(
            @NotNull Long merchantId,
            @NotNull java.util.Map<String, Object> updates
    ) {}

    public record FetchRequest(
            @NotNull Long merchantId,
            @NotNull List<String> paths
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record UpdateResult(
            boolean      success,
            String       message,
            java.util.Map<String, Object> updatedAttributes,
            java.time.LocalDateTime timestamp
    ) {
        public static UpdateResult success(String message, java.util.Map<String, Object> updatedAttributes) {
            return new UpdateResult(true, message, updatedAttributes, java.time.LocalDateTime.now());
        }
        public static UpdateResult failure(String reason) {
            return new UpdateResult(false, reason, null, java.time.LocalDateTime.now());
        }
    }

    public record FetchResult(
            boolean      success,
            String       message,
            java.util.Map<String, Object> values,
            java.time.LocalDateTime timestamp
    ) {
        public static FetchResult success(String message, java.util.Map<String, Object> values) {
            return new FetchResult(true, message, values, java.time.LocalDateTime.now());
        }
        public static FetchResult failure(String reason) {
            return new FetchResult(false, reason, null, java.time.LocalDateTime.now());
        }
    }

    public record SeedResult(
            int          pathCount,
            List<String> paths,
            String       status
    ) {}
}
