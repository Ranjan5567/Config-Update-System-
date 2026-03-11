# ConfigSync — Merchant Configuration Update System

A **production-grade, hybrid Java/React** system for AI-powered JSON configuration updates. Features a premium Neo-Dark UI with glassmorphism and real-time backend validation.

---

## 🏗️ Architecture Overview

``` mermaid
graph TD
    A[React Frontend :5173] -->|Analyze & Validate| B[Java Backend :8081]
    A -->|Natural Language| C[AI Service :8000]
    C -->|Proposed SQL| A
    B -->|Metadata & Validation| A
    B -->|Safe Execution| D[(PostgreSQL JSONB)]
```

### Key Components
- **Java Backend (Spring Boot 3.2)**: The authoritative source of truth. Handles metadata seeding, JSON path analysis, deterministic validation, and safe transactional SQL execution.
- **React Frontend (Vite)**: Premium Neo-Dark interface. Features a multi-step chat flow with auto-retry logic, manual correction prompts, and real-time feedback.
- **AI Orchestrator (External)**: Translates natural language into structured `jsonb_set` SQL statements grounded in system metadata.

---

## 📂 Project Structure

```
config-update-system/
├── java-backend/                  # Spring Boot authoritative backend
│   ├── src/main/java/com/configsystem/
│   │   ├── controller/            # MerchantConfigController (Unified REST API)
│   │   ├── service/               # MerchantConfigService (Core Business Logic)
│   │   ├── repository/            # JpaRepositories
│   │   ├── entity/                # Merchant & Global Metadata entities
│   │   └── dto/                   # Api.java (Standardised Records)
│   └── src/main/resources/        # application.properties
│
└── frontend/                      # React / Vite / Premium Design System
    ├── src/
    │   ├── App.jsx                # Main Chat Orchestrator
    │   ├── index.css              # Global Design Tokens (Neo-Dark)
    │   ├── App.css                # Component Styles (Glassmorphism)
    │   └── services/api.js        # Backend Integration Layer
```

---

## 🚀 Core Features

### 1. Deterministic Seeding
The backend walks all merchant configurations to build a global index of available JSON paths and their inferred data types in the `json_metadata` table.

### 2. Multi-Stage Validation
1. **Analysis**: Java checks if a path exists, identifies current values, and pulls constraints.
2. **AI Simulation**: AI generates a value & SQL based on the analysis.
3. **Authoritative Validation**: Java validates the *proposed* value against strict schema constraints (min/max, enum, regex) before the user sees it.

### 3. Safe Execution
SQL `UPDATE` statements are never run blindly. They are parsed and executed as transactional native queries only after passing through the validation gate.

### 4. Premium User Experience
- **Auto-Retry**: If the AI proposes an invalid value, the system automatically asks it to try again.
- **Manual Fix**: If auto-retry fails twice, the user provides the final value manually.
- **Responsive Feedback**: Clear status indicators for `SUCCESS`, `PRESENT_AS_NULL`, or `NOT_FOUND`.

---

## 🛠️ Setup

### PostgreSQL
Requires a `merchants` table with a `config_json` JSONB column and a `json_metadata` table for schema constraints.

### Execution
1. **Java**: `cd java-backend && mvn spring-boot:run`
2. **Frontend**: `cd frontend && npm run dev`
3. **Seeding**: Use the `/api/merchants/metadata/seed-all` endpoint to initialize system metadata.
