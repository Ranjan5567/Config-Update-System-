package com.configsystem.controller;

import com.configsystem.dto.Api;
import com.configsystem.entity.postgres.MerchantConfigMetadata;
import com.configsystem.repository.postgres.MerchantConfigMetadataRepository;
import com.configsystem.service.MerchantConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Configuration Update Endpoints.
 *
 * POST /merchants/metadata/seed-all   Scan all merchants → populate json_metadata
 * GET  /metadata                      View registered paths + types
 * POST /config/update                 Accepts HashMap of updates and modifies JSON tree
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MerchantConfigController {

    private final MerchantConfigService            service;
    private final MerchantConfigMetadataRepository metadataRepo;

    @PostMapping("/merchants/metadata/seed-all")
    public ResponseEntity<Api.SeedResult> seedAll() {
        return ResponseEntity.ok(service.seedAll());
    }

    /**
     * View registered paths + types
     */
    @GetMapping("/metadata")
    public ResponseEntity<List<MerchantConfigMetadata>> getMetadata() {
        return ResponseEntity.ok(metadataRepo.findAll());
    }

    /**
     * Receives a map of paths to new values.
     * Traverses the merchant's config JSON and updates the values.
     */
    @PostMapping("/config/update")
    public ResponseEntity<Api.UpdateResult> update(
            @Valid @RequestBody Api.UpdateRequest req) {
        return ResponseEntity.ok(service.update(req));
    }
}
