# Config Server

A Git-based Configuration Management Server with multi-namespace support for application configuration files.

## 🚨 BREAKING CHANGES NOTICE (v2.0)

**⚠️ Default Namespace Changed**: The default namespace has been changed from `default` to `main`. 
**⚠️ Vault API Simplified**: The vault system has been redesigned from 8+ endpoints to just 3 core methods.

**📋 Migration Checklist for Existing Users:**
- [ ] Create `main` namespace: `POST /namespace/create {"namespace": "main"}`
- [ ] Migrate configs from `default` to `main` namespace (if applicable)
- [ ] Update client apps using old vault endpoints to new simplified API
- [ ] Test Spring Cloud Config integration with new default namespace

**📖 See [Recent Changes & Migration Notes](#5-recent-changes--migration-notes) for detailed migration guide.**

---

## 🏗️ Architecture

The application follows a clean, layered architecture with well-defined service boundaries:

### Service Layer Structure
```
service/
├── cache/           # Cache management operations
│   ├── CacheManagerService
│   └── CacheManagerServiceImpl
├── operation/       # Git repository operations  
│   ├── GitOperationService
│   └── GitOperationServiceImpl
├── repository/      # Configuration file management
│   ├── GitRepositoryService  
│   └── GitRepositoryServiceImpl
├── validation/      # Input validation and security
│   └── ValidationService
├── util/           # Utility operations
│   └── UtilService  
└── environment/    # Environment-specific services
    ├── KubernetesService (sealed interface)
    └── KubernetesServiceImpl
```

### Key Features
- **Multi-namespace isolation** - Each namespace has its own Git repository
- **Version control** - Full Git history for all configuration changes  
- **Optimistic locking** - Prevents concurrent update conflicts
- **Intelligent caching** - Automatic cache preloading for performance
- **Input validation** - Comprehensive security and format validation
- **Audit trail** - Complete history of all configuration changes
- **Simplified Vault System** - AES-256-GCM encrypted secrets with merge-based updates
- **YAML-Vault Integration** - Dynamic secret replacement in configuration files
- **Smart Secret Processing** - Different views for management UI vs client applications

### Technical Highlights
- **Modern Java 21** - Proper sealed interfaces with non-sealed implementations for Spring compatibility
- **Clean Architecture** - SOLID principles with clear separation of concerns  
- **Interface-based Design** - All services follow contract-first approach with sealed contracts
- **Spring Boot 3.5.5** - Latest Spring ecosystem with enhanced performance
- **Caffeine Caching** - High-performance caching with intelligent eviction
- **AOP Logging** - Comprehensive request tracing and performance monitoring
- **Git Integration** - Native JGit implementation for version control

## 📖 API Documentation

This section provides complete REST API documentation with request/response examples and parameter descriptions for all endpoints.

## 🚀 Getting Started

1. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Test API endpoints**: Use the examples below or your preferred REST client

3. **Create a namespace** (required before creating config files):
   ```bash
   curl -X POST http://localhost:8080/config-server/namespace/create \
     -H "Content-Type: application/json" \
     -d '{"namespace": "test"}'
   ```

4. **Create your first configuration file** using the API endpoints below.

⚠️ **BREAKING CHANGE**: The default namespace for Spring Cloud Config has been changed from `default` to `main` to avoid conflicts with reserved namespace names. 

🚨 **Action Required for Existing Users**: If you have existing configurations in a `default` namespace, you must either:
1. Create a `main` namespace and migrate your configs, OR  
2. Update your Spring Boot applications to explicitly specify the namespace using the `label` parameter

---

# API Endpoints

## 1. Configuration Management API
**Base URL:** `/config`

Manages application configuration files with Git version control and namespace isolation.

### 1.1 Create Configuration File

**Endpoint:** `POST /config/create`

Creates a new configuration file with default YAML template and commits it to Git.

**Request Model:**
```json
{
  "action": "create",
  "appName": "user-service",
  "namespace": "production", 
  "path": "/",
  "email": "developer@company.com"
}
```

**Request Fields:**
- `action` (string, required): Must be "create"
- `appName` (string, required): Application name (alphanumeric, dash, underscore only)
- `namespace` (string, required): Target namespace (must exist)
- `path` (string, required): Directory path within namespace (usually "/")
- `email` (string, required): Email for Git commit attribution

**Response Model:**
```json
{
  "message": "Configuration file has been created successfully",
  "filePath": "production/user-service.yml",
  "commitId": "abc123def456789",
  "status": "created"
}
```

**Status Codes:**
- `201` - Configuration file created successfully
- `400` - Invalid request parameters
- `404` - Namespace not found
- `409` - Configuration file already exists
- `500` - Internal server error

---

### 1.2 Fetch Configuration File

**Endpoint:** `POST /config/fetch`

Retrieves the current content of a configuration file.

**Request Model:**
```json
{
  "action": "fetch",
  "appName": "user-service",
  "namespace": "production",
  "path": "/",
  "email": "developer@company.com"
}
```

**Request Fields:**
- `action` (string, required): Must be "fetch"
- `appName` (string, required): Application name
- `namespace` (string, required): Source namespace
- `path` (string, required): Directory path within namespace
- `email` (string, required): Email for audit trail

**Response Model:**
```json
{
  "action": "fetch",
  "appName": "user-service", 
  "namespace": "production",
  "path": "/",
  "content": "server:\n  port: 8080\n\nspring:\n  application:\n    name: user-service\n\ndatabase:\n  host: ${vault:db_host}\n  password: ${vault:db_password}",
  "commitId": "abc123def456789",
  "lastModified": "2024-01-15T10:30:00Z",
  "author": "developer@company.com"
}
```

**Status Codes:**
- `200` - Configuration file retrieved successfully
- `400` - Invalid request parameters
- `404` - Configuration file or namespace not found
- `500` - Internal server error

---

### 1.3 Update Configuration File

**Endpoint:** `POST /config/update`

Updates an existing configuration file with new content and commits changes to Git.

**Request Model:**
```json
{
  "action": "update",
  "appName": "user-service",
  "namespace": "production", 
  "path": "/",
  "content": "server:\n  port: 8081\n\nspring:\n  application:\n    name: user-service\n\ndatabase:\n  host: ${vault:db_host}\n  password: ${vault:db_password}",
  "message": "Update server port to 8081",
  "email": "developer@company.com",
  "commitId": "abc123def456789"
}
```

**Request Fields:**
- `action` (string, required): Must be "update"
- `appName` (string, required): Application name
- `namespace` (string, required): Target namespace
- `path` (string, required): Directory path within namespace
- `content` (string, required): New YAML configuration content
- `message` (string, required): Git commit message
- `email` (string, required): Email for Git commit attribution
- `commitId` (string, optional): Current commit ID for optimistic locking

**Response Model:**
```json
{
  "message": "Configuration file has been updated successfully",
  "filePath": "production/user-service.yml",
  "commitId": "def456abc789012",
  "previousCommitId": "abc123def456789",
  "status": "updated"
}
```

**Status Codes:**
- `200` - Configuration updated successfully
- `400` - Invalid request or YAML content
- `404` - Configuration file not found
- `409` - Commit conflict (file was modified by another user)
- `500` - Internal server error

---

### 1.4 Get Configuration History

**Endpoint:** `POST /config/history`

Retrieves the commit history for a specific configuration file.

**Request Model:**
```json
{
  "action": "history",
  "appName": "user-service",
  "namespace": "production",
  "path": "/",
  "email": "developer@company.com"
}
```

**Response Model:**
```json
{
  "filePath": "production/user-service.yml",
  "commits": [
    {
      "commitId": "def456abc789012",
      "author": "developer@company.com",
      "date": "2024-01-15T14:30:00Z",
      "message": "Update server port to 8081"
    },
    {
      "commitId": "abc123def456789", 
      "author": "admin@company.com",
      "date": "2024-01-15T10:30:00Z",
      "message": "Initial configuration"
    }
  ],
  "totalCommits": 2
}
```

**Status Codes:**
- `200` - History retrieved successfully
- `400` - Invalid request parameters
- `404` - Configuration file not found
- `500` - Internal server error

---

### 1.5 Get Commit Changes

**Endpoint:** `POST /config/changes`

Retrieves detailed changes for a specific commit ID including diff information.

**Request Model:**
```json
{
  "action": "changes",
  "appName": "user-service",
  "namespace": "production",
  "path": "/",
  "email": "developer@company.com",
  "commitId": "def456abc789012"
}
```

**Response Model:**
```json
{
  "commitId": "def456abc789012",
  "message": "Update server port to 8081",
  "author": "developer@company.com",
  "commitTime": "2024-01-15T14:30:00Z",
  "changes": "--- a/production/user-service.yml\n+++ b/production/user-service.yml\n@@ -1,4 +1,4 @@\n server:\n-  port: 8080\n+  port: 8081\n \n spring:"
}
```

**Status Codes:**
- `200` - Commit changes retrieved successfully
- `400` - Invalid commit ID
- `404` - Commit not found
- `500` - Internal server error

---

### 1.6 Delete Configuration File

**Endpoint:** `POST /config/delete`

Deletes an existing configuration file and commits the change to Git.

**Request Model:**
```json
{
  "action": "delete",
  "appName": "user-service",
  "namespace": "production",
  "path": "/",
  "message": "Remove obsolete configuration",
  "email": "developer@company.com"
}
```

**Response Model:**
```json
{
  "message": "Configuration file has been deleted successfully",
  "filePath": "production/user-service.yml",
  "commitId": "ghi789jkl012345",
  "status": "deleted"
}
```

**Status Codes:**
- `200` - Configuration file deleted successfully
- `400` - Invalid request parameters
- `404` - Configuration file not found
- `500` - Internal server error

---

## 2. Namespace Management API
**Base URL:** `/namespace`

Manages configuration namespaces and directory operations with complete isolation.

### 2.1 Create Namespace

**Endpoint:** `POST /namespace/create`

Creates a new namespace directory with Git initialization for configuration isolation.

**Request Model:**
```json
{
  "namespace": "production"
}
```

**Request Fields:**
- `namespace` (string, required): Namespace name (alphanumeric, dash, underscore only)

**Response Model:**
```json
{
  "message": "Namespace has been created successfully and is ready for configuration files",
  "namespace": "production",
  "gitRepository": "/config/production/.git",
  "vaultInitialized": true,
  "status": "created"
}
```

**Status Codes:**
- `201` - Namespace created successfully
- `400` - Invalid namespace name (reserved names: system, admin, dashboard, default, log, root)
- `409` - Namespace already exists
- `500` - Internal server error

⚠️ **Important**: The `default` namespace is reserved. Spring Cloud Config now uses `main` as the default namespace when no label is specified.

---

### 2.2 List All Namespaces

**Endpoint:** `POST /namespace/list`

Retrieves a list of all available namespaces in the configuration server.

**Request Model:**
```json
{}
```

**Response Model:**
```json
[
  "production",
  "staging", 
  "development",
  "test",
  "integration"
]
```

**Status Codes:**
- `200` - Namespaces retrieved successfully
- `500` - Internal server error

---

### 2.3 List Directory Contents

**Endpoint:** `POST /namespace/files`

Retrieves the list of .yml files and subdirectories within a specified directory path in a namespace.

**Request Model:**
```json
{
  "namespace": "production",
  "path": "/"
}
```

**Request Fields:**
- `namespace` (string, required): Target namespace
- `path` (string, required): Directory path to list (e.g., "/", "/services", "/config")

**Response Model:**
```json
[
  "user-service.yml",
  "payment-service.yml", 
  "api-gateway.yml",
  "services/",
  "config/"
]
```

**Status Codes:**
- `200` - Directory contents retrieved successfully
- `400` - Invalid request parameters
- `404` - Namespace or directory not found
- `500` - Internal server error

---

### 2.4 Delete Namespace

**Endpoint:** `POST /namespace/delete`

Deletes an existing namespace directory and all its contents permanently.

**Request Model:**
```json
{
  "namespace": "old-environment"
}
```

**Response Model:**
```json
{
  "message": "Namespace has been deleted successfully",
  "namespace": "old-environment",
  "filesDeleted": 15,
  "status": "deleted"
}
```

**Status Codes:**
- `200` - Namespace deleted successfully
- `400` - Invalid namespace name or reserved namespace
- `404` - Namespace not found
- `500` - Internal server error

---

## 3. Vault Management API (Simplified)
**Base URL:** `/api/vault/{namespace}`

🆕 **Simplified Design**: The vault system has been redesigned with just 3 core endpoints for better usability. Secrets are stored in `.vault/{namespace}-vault.json` files with AES-256-GCM encryption.

### 3.1 Get Vault Secrets

**Endpoint:** `GET /api/vault/{namespace}`

Retrieve all decrypted secrets from the namespace vault.

**URL Parameters:**
- `namespace` (string, required): Source namespace for the vault

**Request:** No request body required

**Response Model:**
```json
{
  "database.password": "mysecret123",
  "api.token": "token456",
  "jwt.secret": "signing-key-789"
}
```

**Status Codes:**
- `200` - Vault retrieved successfully (returns `{}` if no secrets exist)
- `404` - Namespace not found
- `500` - Internal server error

---

### 3.2 Update Vault Secrets (Merge-based)

**Endpoint:** `PUT /api/vault/{namespace}?email={email}&commitMessage={message}`

Update vault secrets using merge-based approach. New secrets are added, existing ones are updated.

**URL Parameters:**
- `namespace` (string, required): Target namespace for the vault
- `email` (string, required): Email address for Git commit attribution
- `commitMessage` (string, required): Git commit message

**Request Model:**
```json
{
  "database.password": "new_secure_password",
  "api.token": "updated_token_123",
  "new.secret": "brand_new_value"
}
```

**Response Model:**
```json
{
  "message": "Vault updated successfully",
  "namespace": "production",
  "secretsUpdated": 2,
  "secretsAdded": 1,
  "commitId": "abc123def456789"
}
```

**Status Codes:**
- `200` - Vault updated successfully
- `400` - Invalid request parameters (missing email/commitMessage)
- `404` - Namespace not found
- `500` - Internal server error

---

### 3.3 Get Vault History

**Endpoint:** `GET /api/vault/{namespace}/history`

Retrieve the Git commit history of vault changes for audit purposes.

**URL Parameters:**
- `namespace` (string, required): Target namespace

**Response Model:**
```json
{
  "namespace": "production",
  "vaultFile": ".vault/production-vault.json",
  "commits": [
    {
      "commitId": "abc123def456789",
      "author": "admin@company.com",
      "date": "2024-01-15T14:30:00Z",
      "commitMessage": "Update database secrets"
    }
  ],
  "totalCommits": 1
}
```

**Status Codes:**
- `200` - Vault history retrieved successfully
- `404` - Namespace not found
- `500` - Internal server error

---

## 4. YAML-Vault Integration

🆕 **Smart Secret Processing**: The system now automatically integrates vault secrets with YAML configuration files using dynamic replacement.

### How It Works

1. **Configuration Files**: Store your YAML configs with vault key references:
   ```yaml
   server:
     port: 8080
   
   database:
     password: database.password  # This key exists in vault
     host: database.host         # This key exists in vault
   
   regular:
     setting: "normal value"     # Regular config value
   ```

2. **Management UI Response**: When fetched via management APIs, vault keys are replaced with placeholders:
   ```yaml
   server:
     port: 8080
   
   database:
     password: <ENCRYPTED_VALUE>
     host: <ENCRYPTED_VALUE>
   
   regular:
     setting: "normal value"
   ```

3. **Client Application Response**: When fetched via Spring Cloud Config, vault keys are replaced with actual decrypted values:
   ```yaml
   server:
     port: 8080
   
   database:
     password: "actual_secret_password_123"
     host: "prod-db.company.com"
   
   regular:
     setting: "normal value"
   ```

### Secret Key Detection

The system automatically detects vault keys in YAML files using pattern matching for:
- Keys containing: `password`, `secret`, `key`, `token`, `credential`, `auth`
- API keys: `api_key`, `api-key`, `apikey`
- Private keys: `private_key`, `private-key`
- Access keys: `access_key`, `access-key`

### Spring Cloud Config Integration

**Endpoint Format**: `GET /{application}/{profile}/{label}`
- `application`: Your app name
- `profile`: Environment profile (default, dev, prod, etc.)
- `label`: Namespace (defaults to `main` if not specified)

**Example**:
```bash
# Get config for 'api' application, 'default' profile, 'production' namespace
GET /config-server/api/default/production
```

---

## Cache Management

The application automatically preloads namespaces and directory listings on startup for better performance.

---

## 5. Recent Changes & Migration Notes

### 🔄 Vault System Simplification (v2.0)

**What Changed:**
- Reduced from 8+ complex vault endpoints to just 3 core methods
- Removed individual secret operations (create/update/delete single keys)
- Implemented merge-based updates for better UX
- Changed vault storage from `.vault-secrets.json` to `.vault/{namespace}-vault.json`

**Migration Guide:**
- Old bulk operations → Use new `PUT /api/vault/{namespace}` with merge behavior
- Old individual secret gets → Use `GET /api/vault/{namespace}` and extract needed keys
- Old vault history → Use new `GET /api/vault/{namespace}/history`

### 🔄 Namespace Changes (v2.0)

**What Changed:**
- Default namespace changed from `default` to `main` for Spring Cloud Config
- `default` is now a reserved namespace name
- Spring Cloud Config URLs now default to `main` namespace when no label provided

**Migration Impact:**
- **🔴 BREAKING**: Existing Spring Boot applications using default config server will now fetch from `main` namespace instead of `default`
- **Action Required**: Create a `main` namespace and migrate configs from `default` namespace if needed
- **Alternative**: Update client applications to specify label explicitly if using other namespaces

**Client Configuration Updates:**
```yaml
# Before (implicit default namespace)
spring:
  cloud:
    config:
      uri: http://config-server:8080/config-server

# After - Option 1: Use main namespace (recommended)
spring:
  cloud:
    config:
      uri: http://config-server:8080/config-server
      label: main

# After - Option 2: Explicitly specify your namespace
spring:
  cloud:
    config:
      uri: http://config-server:8080/config-server  
      label: production  # or your specific namespace
```

### 🔄 YAML-Vault Integration (v2.0)

**New Feature:**
- Automatic vault key detection and replacement in YAML files
- Different processing for management UI (`<ENCRYPTED_VALUE>`) vs client apps (actual secrets)
- Pattern-based secret key detection (password, secret, key, token, etc.)

---

## Configuration

### Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `SERVER_SERVLET_CONTEXT_PATH` | Application context path | `/config-server` |
| `CONFIG_BASE_PATH` | Base directory for namespace repositories | `/config/` |
| `CONFIG_COMMIT_HISTORY_SIZE` | Maximum commits returned in history API | `10` |
| `VAULT_ENABLED` | Enable vault functionality | `true` |
| `VAULT_CACHE_TTL` | Vault cache time-to-live in seconds | `600` |
| `VAULT_MAX_SECRETS_PER_OPERATION` | Maximum secrets per bulk operation | `100` |

## Docker

```bash
# Build image
docker build -t config-server-image .

# Run container
docker run --name config-server -p 8080:8080 config-server-image
```