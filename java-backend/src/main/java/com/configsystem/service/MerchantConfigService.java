package com.configsystem.service;

import com.configsystem.dto.Api;
import com.configsystem.entity.mysql.Merchant;
import com.configsystem.entity.postgres.MerchantConfigMetadata;
import com.configsystem.repository.postgres.MerchantConfigMetadataRepository;
import com.configsystem.repository.mysql.MerchantRepository;
import com.configsystem.util.JsonPathUtil;
import com.configsystem.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantConfigService {

    private final MerchantRepository               merchantRepo;
    private final MerchantConfigMetadataRepository metadataRepo;
    private final JsonPathUtil                     jsonPathUtil;
    private final ObjectMapper                     objectMapper;

    // ── Seed Metadata ──────────────────────────────────────────────────────────

    @Transactional("postgresTransactionManager")
    public Api.SeedResult seedAll() {
        List<Merchant> merchants = merchantRepo.findAll();
        log.info("Seed-all: scanning {} merchants", merchants.size());

        Map<String, String> pathToType = new LinkedHashMap<>();
        for (Merchant m : merchants) {
            String json = m.getConfigJson();
            if (json == null || json.isBlank()) continue;
            try {
                jsonPathUtil.extractLeafNodes(json)
                        .forEach(leaf -> pathToType.putIfAbsent(leaf.path(), leaf.type()));
            } catch (Exception ex) {
                log.error("Merchant {} parse failed: {}", m.getMerchantId(), ex.getMessage());
            }
        }

        metadataRepo.deleteAllInBatch();
        List<MerchantConfigMetadata> rows = pathToType.entrySet().stream()
                .map(e -> MerchantConfigMetadata.builder()
                        .jsonPath(e.getKey()).dataType(e.getValue()).build())
                .toList();
        metadataRepo.saveAll(rows);

        List<String> paths = new ArrayList<>(pathToType.keySet());
        log.info("Seed-all complete: {} paths from {} merchants", paths.size(), merchants.size());
        return new Api.SeedResult(paths.size(), paths,
                "OK — extracted from " + merchants.size() + " merchants");
    }

    // ── Update ─────────────────────────────────────────────────────────────
    @Transactional("mysqlTransactionManager")
    public Api.UpdateResult update(Api.UpdateRequest req) {
        log.info("Updating config for merchant: {} with {} updates", req.merchantId(), req.updates().size());
        
        try {
            // 1. Fetch Merchant
            Merchant merchant = merchantRepo.findById(req.merchantId())
                    .orElseThrow(() -> new GlobalExceptionHandler.MerchantNotFoundException(req.merchantId()));

            String configJson = merchant.getConfigJson();
            JsonNode root;
            if (configJson == null || configJson.isBlank()) {
                root = objectMapper.createObjectNode();
            } else {
                root = objectMapper.readTree(configJson);
            }

            if (!root.isObject()) {
                throw new RuntimeException("Config JSON is not an object");
            }

            com.fasterxml.jackson.databind.node.ObjectNode objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) root;

            // 2. Validate that all paths exist in json_metadata table
            for (String path : req.updates().keySet()) {
                if (!metadataRepo.existsById(path)) {
                    return Api.UpdateResult.failure("Invalid update: path '" + path + "' does not exist in metadata.");
                }
            }

            // 3. Apply updates
            for (Map.Entry<String, Object> entry : req.updates().entrySet()) {
                updateJsonNode(objectNode, entry.getKey(), entry.getValue());
            }

            merchant.setConfigJson(objectMapper.writeValueAsString(root));
            merchantRepo.save(merchant);

            return Api.UpdateResult.success("Configuration updated successfully.", req.updates());
        } catch (GlobalExceptionHandler.MerchantNotFoundException e) {
             log.warn("Update failed: {}", e.getMessage());
             return Api.UpdateResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Update failed", e);
            return Api.UpdateResult.failure("Update failed: " + e.getMessage());
        }
    }

    private void updateJsonNode(com.fasterxml.jackson.databind.node.ObjectNode root, String path, Object value) {
        String[] parts = path.split("\\.");
        com.fasterxml.jackson.databind.node.ObjectNode current = root;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonNode next = current.get(part);
            if (next == null || !next.isObject()) {
                next = current.putObject(part);
            }
            current = (com.fasterxml.jackson.databind.node.ObjectNode) next;
        }

        String lastPart = parts[parts.length - 1];
        if (value == null) {
            current.putNull(lastPart);
        } else if (value instanceof Boolean v) {
            current.put(lastPart, v);
        } else if (value instanceof Integer v) {
            current.put(lastPart, v);
        } else if (value instanceof Long v) {
            current.put(lastPart, v);
        } else if (value instanceof Double v) {
            current.put(lastPart, v);
        } else if (value instanceof String v) {
            current.put(lastPart, v);
        } else {
            current.set(lastPart, objectMapper.valueToTree(value));
        }
    }
}
