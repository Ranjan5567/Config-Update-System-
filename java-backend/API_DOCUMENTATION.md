# ConfigSync — API Documentation

> **Base URL:** `http://localhost:8081/api`  
> **Swagger UI:** [http://localhost:8081/api/swagger-ui/index.html](http://localhost:8081/api/swagger-ui/index.html)  

---

## Architecture Overview

The system uses **MySQL** to store merchant details and audit logs.
Environment variables are loaded from a `.env` file.

| Database | Table | Purpose |
| :--- | :--- | :--- |
| **MySQL** | `merchants` | Stores merchant records with JSON config |
| **MySQL** | `audit_logs` | Stores audit history of configuration changes |

---

## Database Schema

### MySQL — `merchants` table

| Column | Type | Description |
| :--- | :--- | :--- |
| `merchant_id` | BIGINT (PK) | Unique merchant identifier |
| `config_json` | JSON | Full merchant configuration (includes merchant name, settings, etc.) |

### MySQL — `audit_logs` table

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | BIGINT (PK, AI) | Auto-increment identifier |
| `created_by` | VARCHAR(255) | Who made the change |
| `created_at` | DATETIME | When the change was made |
| `merchant_id` | BIGINT | To which merchant the change was made |
| `attribute_changed` | VARCHAR(255) | Which attribute changed (e.g. "display_config.theme") |
| `value_from` | TEXT | Old value |
| `value_to` | TEXT | New value |

---

## API Endpoints

### 1. Get Current Attribute Value

Retrieves a specific value from a merchant's configuration JSON.

```
POST /api/config/attribute-value
```

#### Request Body
```json
{
  "merchantId": 1001,
  "attribute": "payment_config.interest_rate"
}
```

#### Response (Success)
```json
{
  "success": true,
  "attribute": "payment_config.interest_rate",
  "value": 12.5,
  "message": "Success"
}
```

---

### 2. Update DB with new value

Updates a specific attribute in a merchant's configuration.

```
POST /api/config/update-value
```

#### Request Body
```json
{
  "createdBy": "rm_01",
  "merchantId": 1001,
  "attributeChanged": "payment_config.interest_rate",
  "valueFrom": "12.5",
  "valueTo": 14.5
}
```

#### Response
```json
{
  "success": true,
  "message": "Successfully updated payment_config.interest_rate and stored audit log."
}
```

---

### 3. Retrieve All Merchants Details

Returns a list of all merchants registered in the system.

```
GET /api/merchants/details
```

#### Response
```json
[
  {
    "id": 1,
    "name": "Merchant One"
  },
  {
    "id": 2,
    "name": "Merchant Two"
  }
]
```

---

### 4. Store Audit Logs

Manually records an audit entry.

```
POST /api/audit/store
```

#### Request Body
```json
{
  "createdBy": "rm_01",
  "merchantId": 1001,
  "attributeChanged": "display_config.theme",
  "valueFrom": "#000080",
  "valueTo": "#000000"
}
```

#### Response
```json
{
  "success": true,
  "message": "Success"
}
```

---

### 5. Retrieve Audit Logs

Executes a SELECT query on the audit logs table.

```
POST /api/audit/retrieve
```

#### Request Body
```json
{
  "query": "SELECT * FROM audit_logs WHERE merchant_id = 1"
}
```

#### Response
```json
[
  {
    "id": 1,
    "created_by": "admin_user",
    "created_at": "2026-03-12T18:30:00",
    "merchant_id": 1,
    "attribute_changed": "display_config.theme",
    "value_from": "#000080",
    "value_to": "#000000"
  }
]
```

---

## Configuration

The application requires a `.env` file in the root directory:

```env
DB_URL=jdbc:mysql://localhost:3306/merchant_db
DB_USERNAME=root
DB_PASSWORD=your_password
DB_NAME=merchant_db
```
