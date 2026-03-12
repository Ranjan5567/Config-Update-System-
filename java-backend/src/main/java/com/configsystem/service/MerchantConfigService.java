package com.configsystem.service;

import com.configsystem.dto.Api;
import com.configsystem.entity.mysql.AuditLog;
import com.configsystem.entity.mysql.Merchant;
import com.configsystem.repository.mysql.AuditLogRepository;
import com.configsystem.repository.mysql.MerchantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantConfigService {

    private final MerchantRepository merchantRepo;
    private final AuditLogRepository auditLogRepo;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // API 1: Get Current Attribute Value
    public Api.AttributeValueResponse getCurrentAttributeValue(Long merchantId, String attribute) {
        return merchantRepo.findById(merchantId).map(merchant -> {
            try {
                JsonNode root = objectMapper.readTree(merchant.getConfigJson());
                String pointerStr = "/" + attribute.replace(".", "/");
                JsonNode valNode = root.at(pointerStr);

                if (valNode.isMissingNode() || valNode.isNull()) {
                    return Api.AttributeValueResponse.ok(attribute, null);
                }
                return Api.AttributeValueResponse.ok(attribute, getValueFromNode(valNode));
            } catch (Exception e) {
                log.error("Error parsing JSON for merchant {}", merchantId, e);
                return Api.AttributeValueResponse.error("Error parsing configuration");
            }
        }).orElse(Api.AttributeValueResponse.error("Merchant not found"));
    }

    // API 2: Update DB with new value
    @Transactional
    public Api.GenericResponse updateValue(Api.UpdateValueRequest req) {
        return merchantRepo.findById(req.merchantId()).map(merchant -> {
            try {
                JsonNode root = objectMapper.readTree(merchant.getConfigJson());
                
                ObjectNode objectNode;
                if (root instanceof ObjectNode) {
                    objectNode = (ObjectNode) root;
                } else {
                    objectNode = objectMapper.createObjectNode();
                }

                updateJsonNode(objectNode, req.attributeChanged(), req.valueTo());
                merchant.setConfigJson(objectMapper.writeValueAsString(objectNode));
                merchantRepo.save(merchant);

                return Api.GenericResponse.ok("Successfully updated " + req.attributeChanged());
            } catch (Exception e) {
                log.error("Error updating merchant {}", req.merchantId(), e);
                return Api.GenericResponse.error("Update failed: " + e.getMessage());
            }
        }).orElse(Api.GenericResponse.error("Merchant not found"));
    }

    // API 3: Retrieve All Merchants Details
    public List<Api.MerchantDetail> getAllMerchants() {
        return merchantRepo.findAll().stream()
                .map(m -> {
                    String name = "Unknown";
                    try {
                        JsonNode root = objectMapper.readTree(m.getConfigJson());
                        // Try typical keys for merchant name
                        if (root.has("business_name")) {
                            name = root.get("business_name").asText();
                        } else if (root.has("name")) {
                            name = root.get("name").asText();
                        } else if (root.has("merchant_name")) {
                            name = root.get("merchant_name").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse JSON for merchant {}", m.getMerchantId());
                    }
                    return new Api.MerchantDetail(m.getMerchantId(), name);
                })
                .collect(Collectors.toList());
    }

    // API 4: Store Audit Logs in MySQL
    public Api.GenericResponse storeAuditLog(Api.StoreAuditLogRequest req) {
        try {
            AuditLog log = AuditLog.builder()
                    .createdBy(req.createdBy())
                    .createdAt(LocalDateTime.now())
                    .merchantId(req.merchantId())
                    .attributeChanged(req.attributeChanged())
                    .valueFrom(req.valueFrom())
                    .valueTo(req.valueTo())
                    .build();
            auditLogRepo.save(log);
            return Api.GenericResponse.ok("Audit log stored successfully for merchant " + req.merchantId());
        } catch (Exception e) {
            log.error("Error storing audit log", e);
            return Api.GenericResponse.error("Failed to store audit log");
        }
    }

    // API 5: Retrieve Audit Logs
    public List<Map<String, Object>> retrieveAuditLogs(String query) {
        String upperQuery = query.trim().toUpperCase();
        
        // Basic validation: must be SELECT, no write operations
        if (!upperQuery.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        if (upperQuery.contains("DELETE") || upperQuery.contains("UPDATE") || 
            upperQuery.contains("INSERT") || upperQuery.contains("DROP") || 
            upperQuery.contains("ALTER") || upperQuery.contains("TRUNCATE")) {
            throw new IllegalArgumentException("Unauthorized SQL keywords detected");
        }
        
        // Further validation: only audit_logs table (optional but safer)
        if (!upperQuery.contains("AUDIT_LOGS")) {
             throw new IllegalArgumentException("Queries must target the audit_logs table");
        }

        return jdbcTemplate.queryForList(query);
    }

    private Object getValueFromNode(JsonNode node) {
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNumber()) return node.numberValue();
        if (node.isTextual()) return node.asText();
        return node;
    }

    private void updateJsonNode(ObjectNode root, String path, Object value) {
        String[] parts = path.split("\\.");
        ObjectNode current = root;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonNode next = current.get(part);
            if (next == null || !next.isObject()) {
                next = current.putObject(part);
            }
            current = (ObjectNode) next;
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
