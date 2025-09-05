# Config Server

A Git-based Configuration Management Server with multi-namespace support for application configuration files.

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

## 3. Vault Management API
**Base URL:** `/api/vault/{namespace}`

Manages encrypted secrets in Git-based vaults with complete namespace isolation and AES-256-GCM encryption.

### 3.1 Store Secret

**Endpoint:** `POST /api/vault/{namespace}/secrets`

Store a new encrypted secret in the specified namespace vault.

**URL Parameters:**
- `namespace` (string, required): Target namespace for the vault

**Request Model:**
```json
{
  "key": "db_password",
  "value": "super_secure_password_123",
  "email": "admin@company.com",
  "commitMessage": "Add database password for production environment"
}
```

**Request Fields:**
- `key` (string, required): Unique identifier for the secret (1-255 characters)
- `value` (string, required): Plain text value to be encrypted (1-10000 characters)
- `email` (string, required): Email address for Git commit attribution
- `commitMessage` (string, required): Git commit message (1-500 characters)

**Response Model:**
```json
{
  "message": "Secret stored successfully",
  "namespace": "production",
  "key": "db_password",
  "encrypted": true,
  "commitId": "xyz789abc012def",
  "status": "created"
}
```

**Status Codes:**
- `201` - Secret stored successfully
- `400` - Invalid request parameters or validation failure
- `404` - Namespace not found
- `409` - Secret already exists (use update instead)
- `500` - Internal server error

---

### 3.2 Store Bulk Secrets

**Endpoint:** `POST /api/vault/{namespace}/secrets/bulk`

Store multiple secrets in a single atomic Git commit operation.

**Request Model:**
```json
{
  "secrets": {
    "db_host": "prod-db.company.com",
    "db_port": "5432", 
    "db_username": "app_user",
    "db_password": "secure_password_123",
    "api_key": "sk-1234567890abcdef"
  },
  "email": "devops@company.com",
  "commitMessage": "Initial database and API configuration setup"
}
```

**Request Fields:**
- `secrets` (object, required): Map of secret keys to their plain text values
- `email` (string, required): Email address for Git commit attribution  
- `commitMessage` (string, required): Git commit message for all secrets

**Response Model:**
```json
{
  "message": "Bulk secrets stored successfully",
  "namespace": "production", 
  "count": 5,
  "keys": ["db_host", "db_port", "db_username", "db_password", "api_key"],
  "commitId": "bulk456def789abc",
  "status": "created"
}
```

**Status Codes:**
- `201` - Bulk secrets stored successfully
- `400` - Invalid request or exceeds batch size limits (max 100 secrets)
- `404` - Namespace not found
- `500` - Internal server error

---

### 3.3 Get Secret

**Endpoint:** `POST /api/vault/{namespace}/secrets/get/{key}`

Retrieve a decrypted secret value from the vault.

**URL Parameters:**
- `namespace` (string, required): Source namespace
- `key` (string, required): Secret identifier

**Request Model:**
```json
{}
```

**Response Model:
```json
{
  "namespace": "production",
  "key": "db_password", 
  "value": "super_secure_password_123",
  "lastModified": "2024-01-15T10:30:00Z",
  "author": "admin@company.com"
}
```

**Status Codes:**
- `200` - Secret retrieved successfully
- `400` - Invalid request parameters
- `404` - Secret or namespace not found
- `500` - Internal server error

---

### 3.4 Get All Secrets

**Endpoint:** `POST /api/vault/{namespace}/secrets/list`

Retrieve all decrypted secrets from the namespace vault. Use with caution.

**Request Model:**
```json
{}
```

**Response Model:
```json
{
  "namespace": "production",
  "secrets": {
    "db_password": "super_secure_password_123",
    "api_key": "sk-1234567890abcdef", 
    "jwt_secret": "my-jwt-signing-secret"
  },
  "count": 3,
  "lastModified": "2024-01-15T14:30:00Z"
}
```

**Status Codes:**
- `200` - All secrets retrieved successfully
- `400` - Invalid request parameters
- `404` - Namespace not found
- `500` - Internal server error

---

### 3.5 Update Secret

**Endpoint:** `PUT /api/vault/{namespace}/secrets/{key}`

Update an existing encrypted secret in the vault.

**URL Parameters:**
- `namespace` (string, required): Target namespace
- `key` (string, required): Secret identifier to update

**Request Model:**
```json
{
  "key": "db_password",
  "value": "new_super_secure_password_456", 
  "email": "admin@company.com",
  "commitMessage": "Update database password for security rotation"
}
```

**Response Model:**
```json
{
  "message": "Secret updated successfully",
  "namespace": "production",
  "key": "db_password",
  "previousCommitId": "old123abc456def",
  "commitId": "new789def012abc", 
  "status": "updated"
}
```

**Status Codes:**
- `200` - Secret updated successfully
- `400` - Invalid request parameters
- `404` - Secret or namespace not found
- `500` - Internal server error

---

### 3.6 Delete Secret

**Endpoint:** `DELETE /api/vault/{namespace}/secrets/{key}`

Delete a secret from the vault permanently.

**URL Parameters:**
- `namespace` (string, required): Target namespace
- `key` (string, required): Secret identifier to delete

**Request Model:**
```json
{
  "email": "admin@company.com",
  "commitMessage": "Remove deprecated API key"
}
```

**Response Model:**
```json
{
  "message": "Secret deleted successfully",
  "namespace": "production", 
  "key": "old_api_key",
  "commitId": "del456abc789def",
  "status": "deleted"
}
```

**Status Codes:**
- `200` - Secret deleted successfully
- `400` - Invalid request parameters
- `404` - Secret or namespace not found  
- `500` - Internal server error

---

### 3.7 Check Secret Exists

**Endpoint:** `POST /api/vault/{namespace}/secrets/exists/{key}`

Check if a specific secret exists in the vault without retrieving its value.

**URL Parameters:**
- `namespace` (string, required): Target namespace
- `key` (string, required): Secret identifier to check

**Request Model:**
```json
{}
```

**Response Model:
```json
{
  "namespace": "production",
  "key": "db_password",
  "exists": true
}
```

**Status Codes:**
- `200` - Secret existence check completed
- `400` - Invalid request parameters
- `500` - Internal server error

---

### 3.8 Get Vault History

**Endpoint:** `POST /api/vault/{namespace}/secrets/history`

Retrieve the Git commit history of vault changes for audit and compliance purposes.

**URL Parameters:**
- `namespace` (string, required): Target namespace

**Request Model:**
```json
{}
```

**Response Model:
```json
{
  "namespace": "production",
  "vaultFile": ".vault-secrets.json",
  "commits": [
    {
      "commitId": "latest123abc456def",
      "author": "admin@company.com", 
      "email": "admin@company.com",
      "date": "2024-01-15T14:30:00Z",
      "commitMessage": "Update database password for security rotation"
    },
    {
      "commitId": "prev456def789abc",
      "author": "devops@company.com",
      "email": "devops@company.com", 
      "date": "2024-01-10T09:15:00Z",
      "commitMessage": "Initial database configuration setup"
    }
  ],
  "totalCommits": 2
}
```

**Status Codes:**
- `200` - Vault history retrieved successfully
- `400` - Invalid request parameters
- `404` - Namespace not found
- `500` - Internal server error

---

## Cache Management

The application automatically preloads namespaces and directory listings on startup for better performance.

## Configuration

### Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `SERVER_SERVLET_CONTEXT_PATH` | Application context path | `/config-server` |
| `CONFIG_BASE_PATH` | Base directory for namespace repositories | `/config/` |
| `CONFIG_COMMIT_HISTORY_SIZE` | Maximum commits returned in history API | `10` |

## Docker

```bash
# Build image
docker build -t config-server-image .

# Run container
docker run --name config-server -p 8080:8080 config-server-image
```