# ConfigSync — API Documentation

> **Base URL:** `http://localhost:8081/api`  
> **Swagger UI:** [http://localhost:8081/api/swagger-ui/index.html](http://localhost:8081/api/swagger-ui/index.html)  
> **OpenAPI JSON:** [http://localhost:8081/api/v3/api-docs](http://localhost:8081/api/v3/api-docs)

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Database Schema](#database-schema)
3. [API Endpoints](#api-endpoints)
   - [POST /merchants/metadata/seed-all](#1-seed-metadata)
   - [GET /metadata](#2-get-metadata)
   - [POST /config/update](#3-update-config)
   - [POST /config/fetch](#4-fetch-config)
4. [Error Handling](#error-handling)
5. [Execution Flow Diagrams](#execution-flow-diagrams)


The system uses **two databases**:

| Database   | Purpose                               | Table            |
|------------|---------------------------------------|------------------|
| **MySQL**      | Stores merchant records with JSON config | `merchants`      |
| **PostgreSQL** | Stores extracted JSON path metadata       | `json_metadata`  |

---

## Database Schema

### MySQL — `merchants` table

| Column        | Type    | Description                              |
|---------------|---------|------------------------------------------|
| `merchant_id` | BIGINT (PK) | Unique merchant identifier            |
| `config_json` | JSON    | Full merchant configuration as JSON tree |

**Example `config_json`:**
```json
{
  "payment_config": {
    "interest_rate": 12.5,
    "max_tenure": 24,
    "processing_fee": 299
  },
  "display_config": {
    "show_offers": true,
    "theme": "dark"
  }
}
```

### PostgreSQL — `json_metadata` table

| Column      | Type         | Description                                        |
|-------------|--------------|----------------------------------------------------|
| `json_path` | VARCHAR(1024) (PK) | Dot-notation path (e.g. `payment_config.interest_rate`) |
| `data_type` | VARCHAR(32)  | Inferred type: `STRING`, `NUMBER`, `BOOLEAN`, `NULL` |

---

## API Endpoints

---

### 1. Seed Metadata

Scans **all** merchants in MySQL, extracts every leaf-node JSON path from their `config_json`, and populates the `json_metadata` table in PostgreSQL.

```
POST /api/merchants/metadata/seed-all
```

#### Request

- **Headers:** `Content-Type: application/json`
- **Body:** _None_ (no request body required)

#### Response — `200 OK`

```json
{
  "pathCount": 5,
  "paths": [
    "payment_config.interest_rate",
    "payment_config.max_tenure",
    "payment_config.processing_fee",
    "display_config.show_offers",
    "display_config.theme"
  ],
  "status": "OK — extracted from 3 merchants"
}
```

| Field       | Type     | Description                                   |
|-------------|----------|-----------------------------------------------|
| `pathCount` | Integer  | Total number of unique JSON paths discovered   |
| `paths`     | String[] | List of all dot-notation paths                 |
| `status`    | String   | Human-readable summary                         |

#### Execution Flow

```
Client
  │
  ▼
POST /merchants/metadata/seed-all
  │
  ▼
MerchantConfigController.seedAll()
  │
  ▼
MerchantConfigService.seedAll()          @Transactional("postgresTransactionManager")
  │
  ├─── 1. merchantRepo.findAll()         ──▶ MySQL: SELECT * FROM merchants
  │         │
  │         ▼
  │    For each merchant:
  │         │
  │         ├── Skip if config_json is null or blank
  │         │
  │         └── JsonPathUtil.extractLeafNodes(config_json)
  │                  │
  │                  ├── Parse JSON string into a Jackson tree
  │                  ├── Walk tree iteratively (stack-based DFS)
  │                  ├── Collect every leaf node as (path, type)
  │                  │     path  = dot-separated keys (e.g. "payment_config.interest_rate")
  │                  │     type  = STRING | NUMBER | BOOLEAN | NULL
  │                  └── Return List<LeafNode>
  │
  │    Merge all leaf nodes into a LinkedHashMap (deduplicates by path)
  │
  ├─── 2. metadataRepo.deleteAllInBatch()  ──▶ Postgres: DELETE FROM json_metadata
  │
  ├─── 3. metadataRepo.saveAll(rows)       ──▶ Postgres: INSERT INTO json_metadata
  │
  └─── 4. Return SeedResult { pathCount, paths, status }
              │
              ▼
         HTTP 200 OK (JSON response)
```

---

### 2. Get Metadata

Returns all registered JSON paths and their data types from the `json_metadata` table.

```
GET /api/metadata
```

#### Request

- **Headers:** None required
- **Body:** _None_

#### Response — `200 OK`

```json
[
  {
    "jsonPath": "payment_config.interest_rate",
    "dataType": "NUMBER"
  },
  {
    "jsonPath": "payment_config.max_tenure",
    "dataType": "NUMBER"
  },
  {
    "jsonPath": "display_config.show_offers",
    "dataType": "BOOLEAN"
  },
  {
    "jsonPath": "display_config.theme",
    "dataType": "STRING"
  }
]
```

| Field      | Type   | Description                                           |
|------------|--------|-------------------------------------------------------|
| `jsonPath` | String | Dot-notation JSON path                                |
| `dataType` | String | Inferred type: `STRING`, `NUMBER`, `BOOLEAN`, or `NULL` |

#### Execution Flow

```
Client
  │
  ▼
GET /metadata
  │
  ▼
MerchantConfigController.getMetadata()
  │
  ▼
metadataRepo.findAll()               ──▶ PostgreSQL: SELECT * FROM json_metadata
  │
  ▼
Return List<MerchantConfigMetadata>
  │
  ▼
HTTP 200 OK (JSON array)
```

---

### 3. Update Config

Updates one or more JSON paths in a specific merchant's `config_json`. Each path is validated against the `json_metadata` table before being applied.

```
POST /api/config/update
```

#### Request

- **Headers:** `Content-Type: application/json`
- **Body:**

```json
{
  "merchantId": 1001,
  "updates": {
    "payment_config.interest_rate": 14.5,
    "display_config.show_offers": false,
    "display_config.theme": "light"
  }
}
```

| Field        | Type                | Required | Description                                |
|--------------|---------------------|----------|--------------------------------------------|
| `merchantId` | Long                | ✅ Yes   | ID of the merchant to update               |
| `updates`    | Map<String, Object> | ✅ Yes   | Key = dot-notation path, Value = new value |

**Supported value types:** `String`, `Integer`, `Long`, `Double`, `Boolean`, `null`

#### Response — `200 OK` (Success)

```json
{
  "success": true,
  "message": "Configuration updated successfully.",
  "updatedAttributes": {
    "payment_config.interest_rate": 14.5,
    "display_config.show_offers": false,
    "display_config.theme": "light"
  },
  "timestamp": "2026-03-11T18:05:00"
}
```

#### Response — `200 OK` (Failure — invalid path)

```json
{
  "success": false,
  "message": "Invalid update: path 'nonexistent.path' does not exist in metadata.",
  "updatedAttributes": null,
  "timestamp": "2026-03-11T18:05:00"
}
```

#### Response — `200 OK` (Failure — merchant not found)

```json
{
  "success": false,
  "message": "Merchant 9999 not found.",
  "updatedAttributes": null,
  "timestamp": "2026-03-11T18:05:00"
}
```

| Field              | Type                | Description                                      |
|--------------------|---------------------|--------------------------------------------------|
| `success`          | Boolean             | `true` if update succeeded                       |
| `message`          | String              | Human-readable result or error reason            |
| `updatedAttributes`| Map<String, Object> | Echo of applied updates (null on failure)         |
| `timestamp`        | LocalDateTime       | Server timestamp of the operation                |

#### Execution Flow

```
Client
  │
  ▼
POST /config/update  { merchantId: 1001, updates: { ... } }
  │
  ▼
MerchantConfigController.update()
  │  @Valid validates: merchantId != null, updates != null
  │
  ▼
MerchantConfigService.update()          @Transactional("mysqlTransactionManager")
  │
  ├─── 1. FETCH MERCHANT
  │         merchantRepo.findById(merchantId)    ──▶ MySQL: SELECT * FROM merchants
  │                                                        WHERE merchant_id = ?
  │         │
  │         └── Not found? → Return failure("Merchant X not found.")
  │
  ├─── 2. PARSE CONFIG JSON
  │         objectMapper.readTree(merchant.getConfigJson())
  │         │
  │         └── JSON is null/blank? → Start with empty ObjectNode {}
  │
  ├─── 3. VALIDATE PATHS
  │         For each path in updates.keySet():
  │             metadataRepo.existsById(path)    ──▶ Postgres: SELECT 1 FROM json_metadata
  │                                                            WHERE json_path = ?
  │             │
  │             └── Not found? → Return failure("Invalid update: path 'X' does not exist")
  │
  ├─── 4. APPLY UPDATES
  │         For each (path, value) in updates:
  │             updateJsonNode(root, path, value)
  │             │
  │             ├── Split path by "." → ["payment_config", "interest_rate"]
  │             ├── Walk the JSON tree, creating intermediate objects if missing
  │             └── Set the leaf value with the correct type
  │
  ├─── 5. SAVE
  │         merchant.setConfigJson(objectMapper.writeValueAsString(root))
  │         merchantRepo.save(merchant)          ──▶ MySQL: UPDATE merchants
  │                                                        SET config_json = ?
  │                                                        WHERE merchant_id = ?
  │
  └─── 6. Return UpdateResult { success: true, message, updatedAttributes, timestamp }
              │
              ▼
         HTTP 200 OK (JSON response)
```

---

### 4. Fetch Config

Fetches the **current values** of one or more JSON paths from a specific merchant's `config_json`. This is useful for reading config values before applying updates (e.g., to display current state in a review/approval stage).

```
POST /api/config/fetch
```

#### Request

- **Headers:** `Content-Type: application/json`
- **Body:**

```json
{
  "merchantId": 1001,
  "paths": [
    "payment_config.interest_rate",
    "display_config.theme",
    "display_config.show_offers"
  ]
}
```

| Field        | Type           | Required | Description                                     |
|--------------|----------------|----------|-------------------------------------------------|
| `merchantId` | Long           | ✅ Yes   | ID of the merchant to fetch config from         |
| `paths`      | List\<String\> | ✅ Yes   | List of dot-notation JSON paths to retrieve     |

#### Response — `200 OK` (Success)

```json
{
  "success": true,
  "message": "Configuration fetched successfully.",
  "values": {
    "payment_config.interest_rate": 14.5,
    "display_config.theme": "light",
    "display_config.show_offers": false
  },
  "timestamp": "2026-03-11T19:30:00"
}
```

#### Response — `200 OK` (Failure — merchant not found)

```json
{
  "success": false,
  "message": "Merchant 9999 not found.",
  "values": null,
  "timestamp": "2026-03-11T19:30:00"
}
```

> **Note:** If a requested path does not exist in the merchant's JSON, its value will be `null` in the response (the request will still succeed).

| Field       | Type                | Description                                         |
|-------------|---------------------|-----------------------------------------------------|
| `success`   | Boolean             | `true` if fetch succeeded                           |
| `message`   | String              | Human-readable result or error reason               |
| `values`    | Map\<String, Object\> | Requested paths mapped to their current values    |
| `timestamp` | LocalDateTime       | Server timestamp of the operation                   |

#### Execution Flow

```
Client
  │
  ▼
POST /config/fetch  { merchantId: 1001, paths: [ ... ] }
  │
  ▼
MerchantConfigController.fetch()
  │  @Valid validates: merchantId != null, paths != null
  │
  ▼
MerchantConfigService.fetch()          @Transactional("mysqlTransactionManager")
  │
  ├─── 1. FETCH MERCHANT
  │         merchantRepo.findById(merchantId)    ──▶ MySQL: SELECT * FROM merchants
  │                                                        WHERE merchant_id = ?
  │         │
  │         └── Not found? → Return failure("Merchant X not found.")
  │
  ├─── 2. PARSE CONFIG JSON
  │         objectMapper.readTree(merchant.getConfigJson())
  │         │
  │         └── JSON is null/blank? → Start with empty ObjectNode {}
  │
  ├─── 3. RESOLVE EACH PATH
  │         For each path in paths:
  │             Convert dot-notation to JSON Pointer (e.g. "payment_config.interest_rate"
  │                                                     → "/payment_config/interest_rate")
  │             root.at(pointer)
  │             │
  │             ├── Missing/null → values.put(path, null)
  │             ├── Boolean     → values.put(path, booleanValue)
  │             ├── Number      → values.put(path, numberValue)
  │             ├── String      → values.put(path, textValue)
  │             └── Object/Array→ values.put(path, deserializedObject)
  │
  └─── 4. Return FetchResult { success: true, message, values, timestamp }
              │
              ▼
         HTTP 200 OK (JSON response)
```

---

## Error Handling

All errors follow the **RFC 7807 ProblemDetail** format (provided by Spring's built-in `ProblemDetail` class).

### Validation Error — `400 Bad Request`

Triggered when `@Valid` fails (e.g., `merchantId` is null).

```json
{
  "type": "https://configsystem/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "fieldErrors": {
    "merchantId": "must not be null"
  },
  "timestamp": "2026-03-11T12:35:00.000Z"
}
```

### Malformed JSON — `400 Bad Request`

Triggered when the request body is not parseable JSON.

```json
{
  "type": "https://configsystem/errors/bad-json",
  "title": "Malformed JSON",
  "status": 400,
  "detail": "The provided JSON is malformed or invalid.",
  "timestamp": "2026-03-11T12:35:00.000Z"
}
```

### Merchant Not Found — `404 Not Found`

Triggered when a merchant ID is thrown via exception (if the exception escapes the service catch block).

```json
{
  "type": "https://configsystem/errors/not-found",
  "title": "Merchant Not Found",
  "status": 404,
  "detail": "Merchant 9999 not found.",
  "timestamp": "2026-03-11T12:35:00.000Z"
}
```

### Internal Server Error — `500`

Triggered for any unhandled exception.

```json
{
  "type": "https://configsystem/errors/internal",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred.",
  "timestamp": "2026-03-11T12:35:00.000Z"
}
```

---

## Execution Flow Diagrams

### Complete System Flow

```
                    ┌──────────────────────────────────┐
                    │         INITIAL SETUP             │
                    │                                    │
                    │  1. Start the Spring Boot app      │
                    │  2. Both MySQL and PostgreSQL       │
                    │     connections are established     │
                    │  3. Hibernate creates/updates       │
                    │     tables via ddl-auto=update      │
                    └──────────────┬───────────────────┘
                                   │
                    ┌──────────────▼───────────────────┐
                    │         SEED PHASE                 │
                    │                                    │
                    │  POST /merchants/metadata/seed-all │
                    │                                    │
                    │  Reads all merchants from MySQL,   │
                    │  walks their JSON configs,          │
                    │  extracts all unique leaf paths,    │
                    │  and stores them in PostgreSQL's    │
                    │  json_metadata table.               │
                    │                                    │
                    │  ⚠ Must be called before updates   │
                    │    can be validated.                │
                    └──────────────┬───────────────────┘
                                   │
          ┌──────────────┬─────────┼─────────┬──────────────┐
          │              │         │         │              │
┌─────────▼────────┐ ┌───▼─────────▼───┐ ┌───▼──────────┐ ┌▼─────────────────┐
│  GET /metadata   │ │  GET /metadata  │ │ POST /config/│ │ POST /config/    │
│                  │ │                 │ │      update  │ │      fetch       │
│  Browse paths    │ │  (MCP Server    │ │              │ │                  │
│  & types for     │ │   uses this to  │ │  Send updates│ │  Fetch current   │
│  reference       │ │   provide LLM   │ │  as path →   │ │  values for      │
│                  │ │   context)      │ │  value map   │ │  given paths     │
└──────────────────┘ └─────────────────┘ └──────────────┘ └──────────────────┘
```

### Data Flow Across Databases

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   MySQL (merchants)                PostgreSQL (json_metadata)        │
│   ┌─────────────────┐             ┌──────────────────────────┐      │
│   │ merchant_id: 1  │             │                          │      │
│   │ config_json:    │──SEED-ALL──▶│  json_path      data_type│      │
│   │  {              │             │  ──────────────  ─────── │      │
│   │   "payment": {  │             │  payment.rate   NUMBER   │      │
│   │     "rate": 12  │             │  payment.fee    NUMBER   │      │
│   │     "fee": 299  │             │  display.theme  STRING   │      │
│   │   },            │             │                          │      │
│   │   "display": {  │             └───────────┬──────────────┘      │
│   │     "theme":"x" │                         │                      │
│   │   }             │                         │ VALIDATE             │
│   │  }              │                         │                      │
│   │                 │◀────UPDATE──────────────┘                      │
│   │ After update:   │                                                │
│   │  rate → 14.5    │                                                │
│   └─────────────────┘                                                │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Transaction Boundaries

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  seedAll()  → @Transactional("postgresTransactionManager")      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  READ from MySQL (no transaction needed — read-only)    │    │
│  │  DELETE + INSERT into PostgreSQL (transactional)         │    │
│  │                                                         │    │
│  │  If anything fails → PostgreSQL changes roll back       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  update()   → @Transactional("mysqlTransactionManager")         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  READ from PostgreSQL (validation — no write)           │    │
│  │  READ + WRITE to MySQL (transactional)                  │    │
│  │                                                         │    │
│  │  If anything fails → MySQL changes roll back            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  fetch()    → @Transactional("mysqlTransactionManager")         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  READ from MySQL (read-only, transactional)             │    │
│  │  Parse JSON and resolve each requested path             │    │
│  │                                                         │    │
│  │  No writes — safe, idempotent operation                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

| Method | Endpoint                          | Database         | Description                           |
|--------|-----------------------------------|------------------|---------------------------------------|
| POST   | `/api/merchants/metadata/seed-all` | MySQL → Postgres | Scan merchants, populate metadata     |
| GET    | `/api/metadata`                    | Postgres         | List all known JSON paths + types     |
| POST   | `/api/config/update`               | Postgres + MySQL | Validate paths, then update config    |
| POST   | `/api/config/fetch`                | MySQL            | Fetch current values for given paths  |
