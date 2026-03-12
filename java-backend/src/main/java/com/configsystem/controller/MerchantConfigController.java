package com.configsystem.controller;

import com.configsystem.dto.Api;
import com.configsystem.service.MerchantConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MerchantConfigController {

    private final MerchantConfigService service;

    // API 1-Get Current Attribute Value
    @PostMapping("/config/attribute-value")
    public ResponseEntity<Api.AttributeValueResponse> getAttributeValue(@Valid @RequestBody Api.AttributeValueRequest req) {
        return ResponseEntity.ok(service.getCurrentAttributeValue(req.merchantId(), req.attribute()));
    }

    // API 2-Update DB with new value
    @PostMapping("/config/update-value")
    public ResponseEntity<Api.GenericResponse> updateValue(@Valid @RequestBody Api.UpdateValueRequest req) {
        return ResponseEntity.ok(service.updateValue(req));
    }

    // API 3 Retrieve All Merchants Details
    @GetMapping("/merchants/details")
    public ResponseEntity<List<Api.MerchantDetail>> getAllMerchants() {
        return ResponseEntity.ok(service.getAllMerchants());
    }

    // API 4 Store Audit Logs in MySQL
    @PostMapping("/audit/store")
    public ResponseEntity<Api.GenericResponse> storeAuditLog(@Valid @RequestBody Api.StoreAuditLogRequest req) {
        return ResponseEntity.ok(service.storeAuditLog(req));
    }

    // API 5 - Retrieve Audit Logs
    @PostMapping("/audit/retrieve")
    public ResponseEntity<List<Map<String, Object>>> retrieveAuditLogs(@Valid @RequestBody Api.AuditLogQueryRequest req) {
        return ResponseEntity.ok(service.retrieveAuditLogs(req.query()));
    }
}
