# ConfigSync — API Documentation

> **Base URL:** `http://localhost:8081/api`  
> **Swagger UI:** [http://localhost:8081/api/swagger-ui/index.html](http://localhost:8081/api/swagger-ui/index.html)  

---

## Architecture Overview

The system uses **MySQL** to store merchant details and audit logs.
- **Config Updates**: Clients update configuration via the `/config/update-value` endpoint.
- **Audit Logging**: The system **does not** automatically log audits on update. The orchestrator/program must call `/audit/store` after a successful update.
- **SQL Flexibility**: The `/audit/retrieve` endpoint allows raw SELECT queries on the audit table for advanced reporting.

| Database | Table | Purpose |
| :--- | :--- | :--- |
| **MySQL** | `merchants` | Stores merchant records with JSON config |
| **MySQL** | `audit_logs` | Stores audit history of configuration changes |

---

## API Endpoints

### 1. Get Current Attribute Value
`POST /config/attribute-value`

Retrieves a specific value from a merchant's configuration JSON using dot notation (e.g., `ui.theme`).

#### Scenarios
| Scenario | Request | Response Body Structure |
| :--- | :--- | :--- |
| **Success** | `{"merchantId": 1001, "attribute": "active"}` | `{"success": true, "attribute": "active", "value": "true", ...}` |
| **Value is Null** | `{"merchantId": 1001, "attribute": "theme"}` | `{"success": true, "attribute": "theme", "value": null, ...}` |
| **Not Found** | `{"merchantId": 999, "attribute": "active"}` | `{"success": false, "message": "Merchant not found"}` |

#### Example Success Response
```json
{
  "success": true,
  "attribute": "payment_config.interest_rate",
  "value": 12.5,
  "message": "Successfully retrieved the value"
}
```

---

### 2. Update Configuration Value
`POST /config/update-value`

Updates a specific attribute in the merchant's configuration. Supports nesting and will create missing objects in the JSON path.

#### Scenarios
| Scenario | Request | Response Body Structure |
| :--- | :--- | :--- |
| **Success** | `{"merchantId": 1001, "attribute": "active", "value": "false"}` | `{"success": true, "message": "Successfully updated active for merchant 1001"}` |
| **Nested** | `{"merchantId": 1001, "attribute": "ui.theme", "value": "dark"}` | `{"success": true, "message": "Successfully updated ui.theme for merchant 1001"}` |
| **Not Found** | `{"merchantId": 999, ...}` | `{"success": false, "message": "Merchant not found"}` |

#### Example Request Body
```json
{
  "merchantId": 1001,
  "attribute": "payment_config.interest_rate",
  "value": 14.5
}
```

#### Example Response
```json
{
  "success": true,
  "message": "Successfully updated payment_config.interest_rate for merchant 1001"
}
```

---

### 3. Retrieve All Merchant Details
`GET /merchants/details`

Returns basic information for all active merchants.

#### Scenarios
| Scenario | Request | Response Body Structure |
| :--- | :--- | :--- |
| **Success** | `GET /merchants/details` | `[{"merchantId": 1001, "merchantName": "Amazon"}, ...]` |
| **Missing Name**| `GET /merchants/details` | `[{"merchantId": 1003, "merchantName": "Unknown"}]` |
| **Empty DB** | `GET /merchants/details` | `[]` |

#### Example Response
```json
[
  {
    "merchantId": 1,
    "merchantName": "Merchant One"
  },
  {
    "merchantId": 2,
    "merchantName": "Merchant Two"
  }
]
```

---

### 4. Store Audit Logs
`POST /audit/store`

Manual orchestration point to record changes. **Must be triggered by the program after API 2 succeeds.**

#### Scenarios
| Scenario | Request | Response Body Structure |
| :--- | :--- | :--- |
| **Success** | `{"createdBy": "admin", "merchantId": 1001, ...}` | `{"success": true, "message": "Audit log stored successfully..."}` |
| **DB Error** | `{"merchantId": 1001, ...}` | `{"success": false, "message": "Failed to store audit log"}` |

#### Example Request Body
```json
{
  "createdBy": "admin_user",
  "merchantId": 1001,
  "attributeChanged": "display_config.theme",
  "valueFrom": "#000080",
  "valueTo": "#000000"
}
```

---

### 5. Retrieve Audit Logs
`POST /audit/retrieve`

Advanced search using raw SQL queries. **Only SELECT queries on the `audit_logs` table are permitted.**

#### Scenarios
| Scenario | Query Example | Response Body Structure |
| :--- | :--- | :--- |
| **Success** | `SELECT * FROM audit_logs` | `[{"id": 1, "created_at": "...", ...}]` |
| **Unauthorized**| `DELETE FROM audit_logs` | `{"success": false, "message": "Only SELECT queries are allowed"}` |
| **Wrong Table** | `SELECT * FROM merchants` | `{"success": false, "message": "Queries must target the audit_logs table"}` |

#### Example Request Body
```json
{
  "query": "SELECT * FROM audit_logs WHERE merchant_id = 1001 ORDER BY created_at DESC"
}
```

---

## Configuration

Required `.env` file structure:
```env
DB_URL=jdbc:mysql://localhost:3306/merchant_db
DB_USERNAME=root
DB_PASSWORD=your_password
DB_NAME=merchant_db
```
