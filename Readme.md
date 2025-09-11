# Config Server

A Git-based Configuration Management Server with multi-namespace support for application configuration files.

---

## 🏗️ Architecture

The application follows a clean, layered architecture with well-defined service boundaries:

### Service Layer Structure

```
service/
├── cache/           # Cache management operations
│   ├── CacheManagerService
│   └── CacheManagerServiceImpl
├── cloud/           # Cloud Config integration
│   └── CloudConfigService
├── encryption/      # Vault encryption services
│   ├── EncryptionService
│   └── AESEncryptionServiceImpl
├── notify/          # Client notification services
│   ├── ClientNotifyService
│   └── NotificationStorageService
├── operation/       # Git repository operations  
│   ├── GitOperationService
│   └── GitOperationServiceImpl
├── repository/      # Configuration file management
│   ├── GitRepositoryService  
│   └── GitRepositoryServiceImpl
├── secret/          # Secret processing and YAML integration
│   ├── SecretProcessor
│   └── SecretProcessorImpl
├── util/           # Utility operations
│   └── UtilService
├── validation/      # Input validation and security
│   └── ValidationService
└── vault/          # Vault management services
    ├── GitVaultService
    └── GitVaultServiceImpl
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
- **Spring Cloud 2025.0.0** - Latest Spring Cloud release for configuration management
- **Caffeine Caching** - High-performance caching with intelligent eviction
- **AOP Logging** - Comprehensive request tracing and performance monitoring
- **Git Integration** - Native JGit implementation for version control

## 📖 API Documentation

This section provides complete REST API documentation with request/response examples and parameter descriptions for all
endpoints.

## 🚀 Getting Started

### Quick Start (Development)

1. **Start the application** (uses default master key):
   ```bash
   mvn spring-boot:run
   ```
   
   ⚠️ **You'll see security warnings** - this is normal for development.

2. **Create a namespace** (required before creating config files):
   ```bash
   curl -X POST http://localhost:8080/config-server/namespace/create \
     -H "Content-Type: application/json" \
     -d '{"namespace": "test"}'
   ```

3. **Create your first configuration file**:
   ```bash
   curl -X POST http://localhost:8080/config-server/config/create \
     -H "Content-Type: application/json" \
     -d '{
       "action": "create",
       "appName": "my-app", 
       "namespace": "test",
       "path": "/",
       "email": "developer@example.com"
     }'
   ```

4. **Add some vault secrets**:
   ```bash
   curl -X POST http://localhost:8080/config-server/vault/update \
     -H "Content-Type: application/json" \
     -d '{
       "namespace": "test",
       "email": "developer@example.com", 
       "commitMessage": "Add initial secrets",
       "database.password": "mysecretpassword",
       "api.token": "myapitoken123"
     }'
   ```

### Production Setup

1. **Generate a secure master key**:
   ```bash
   export VAULT_MASTER_KEY=$(openssl rand -base64 32)
   echo "Your master key: $VAULT_MASTER_KEY"
   # IMPORTANT: Save this key securely!
   ```

2. **Start the application**:
   ```bash
   VAULT_MASTER_KEY=$VAULT_MASTER_KEY mvn spring-boot:run
   ```
   
   ✅ **You'll see success messages** - no security warnings.

3. **Continue with steps 2-4 above** for creating namespaces and configs.

ℹ️ **Note**: The `default` namespace is reserved for system use. Spring Cloud Config uses `main` as the default namespace when no label is specified.

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

⚠️ **Important**: The `default` namespace is reserved for system use. Spring Cloud Config uses `main` as the default namespace when no label is specified.

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
  "user-service",
  "payment-service",
  "api-gateway",
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
  "message": "Namespace has been deleted successfully"
}
```

**Status Codes:**

- `200` - Namespace deleted successfully
- `400` - Invalid namespace name or reserved namespace
- `404` - Namespace not found
- `500` - Internal server error

---

### 2.5 Get Namespace Events

**Endpoint:** `POST /namespace/events`

Retrieves the complete event history (git log) for an entire namespace. Shows all Git commits and activities within the
namespace root directory.

**Request Model:**

```json
{
  "namespace": "production"
}
```

**Request Fields:**

- `namespace` (string, required): Target namespace name

**Response Model:**

```json
{
  "namespace": "production",
  "commits": [
    {
      "commitId": "abc123def456789",
      "author": "developer@company.com",
      "date": "2024-01-15T14:30:00Z",
      "commitMessage": "Update user-service configuration"
    },
    {
      "commitId": "def456abc789012",
      "author": "admin@company.com",
      "date": "2024-01-15T10:30:00Z",
      "commitMessage": "Initialize production namespace"
    }
  ],
  "totalCommits": 2
}
```

**Status Codes:**

- `200` - Namespace events retrieved successfully
- `400` - Invalid request parameters or namespace name
- `404` - Namespace not found
- `500` - Internal server error

---

### 2.6 Get Namespace Notifications

**Endpoint:** `POST /namespace/notify`

Retrieves API call status notifications for the last `commit-history-size` operations within a namespace. Shows
execution status, timing information, retry counts, and results for configuration management operations.

**Request Model:**

```json
{
  "namespace": "production"
}
```

**Request Fields:**

- `namespace` (string, required): Target namespace name

**Response Model:**

```json
{
  "namespace": "production",
  "notifications": [
    {
      "triggeredAt": "2024-01-15T14:35:00",
      "appName": "user-service",
      "operation": "update",
      "status": "success",
      "retryCount": 0,
      "namespace": "production",
      "commitId": "abc123def456789"
    },
    {
      "triggeredAt": "2024-01-15T14:32:00",
      "appName": "order-service",
      "operation": "create",
      "status": "inprogress",
      "retryCount": 2,
      "namespace": "production",
      "errorMessage": "Network timeout, retrying..."
    },
    {
      "triggeredAt": "2024-01-15T14:25:00",
      "appName": "payment-service",
      "operation": "delete",
      "status": "failed",
      "retryCount": 3,
      "namespace": "production",
      "errorMessage": "Maximum retry attempts exceeded"
    }
  ],
  "totalNotifications": 3,
  "maxNotifications": 10
}
```

**Response Fields:**

- `namespace` (string): The namespace that was queried
- `notifications` (array): List of notification status objects
    - `triggeredAt` (string): When the API call was triggered (ISO format)
    - `appName` (string): Application name from the original payload
    - `operation` (string): Type of operation (create, update, delete, etc.)
    - `status` (string): Current status - `success`, `inprogress`, or `failed`
    - `retryCount` (integer): Number of retry attempts made
    - `namespace` (string): Namespace where the operation occurred
    - `commitId` (string, optional): Associated Git commit ID (if operation succeeded)
    - `errorMessage` (string, optional): Error details (if operation failed or is retrying)
- `totalNotifications` (integer): Number of notifications returned
- `maxNotifications` (integer): Maximum notifications limit (from commit-history-size config)

**Status Descriptions:**

- `success` - Operation completed successfully without errors
- `inprogress` - Operation is currently being retried due to previous failures
- `failed` - Operation failed after all retry attempts were exhausted

**Status Codes:**

- `200` - Namespace notifications retrieved successfully
- `400` - Invalid request parameters or namespace name
- `404` - Namespace not found
- `500` - Internal server error

---

## 3. Vault Management API (Simplified)

**Base URL:** `/vault`

🆕 **Simplified Design**: The vault system has been redesigned with just 2 core endpoints for better usability. Secrets
are stored in `.vault/{namespace}-vault.json` files with AES-256-GCM encryption using a single master key.

🔒 **Enhanced Security**: 
- All vault endpoints use POST requests for better security
- Single master key approach for simplified key management  
- Master key stored as environment variable (not files)
- AES-256-GCM encryption with secure random initialization vectors

### 3.1 Get Vault Secrets

**Endpoint:** `POST /vault/get`

Retrieve all decrypted secrets from the namespace vault.

**Request Model:**

```json
{
  "namespace": "production"
}
```

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

### 3.2 Update Vault Secrets (Complete Replacement)

**Endpoint:** `POST /vault/update`

Update vault secrets using complete replacement approach. All existing secrets are replaced with the provided ones.

**Request Model:**

```json
{
  "namespace": "production",
  "email": "user@example.com",
  "commitMessage": "Update vault secrets",
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
  "count": 3
}
```

**Status Codes:**

- `200` - Vault updated successfully
- `400` - Invalid request parameters (missing email/commitMessage)
- `404` - Namespace not found
- `500` - Internal server error

---

---

## Cache Management

The application automatically preloads namespaces and directory listings on startup for better performance.

---

## Configuration

### Environment Variables

| Variable                          | Description                               | Default Value    |
|-----------------------------------|-------------------------------------------|------------------|
| `SERVER_PORT`                     | HTTP server port                          | `8080`           |
| `SERVER_SERVLET_CONTEXT_PATH`     | Application context path                  | `/config-server` |
| `SPRING_CLOUD_CONFIG_SERVER_PREFIX` | Spring Cloud Config API prefix         | `/config-api`    |
| `CONFIG_BASE_PATH`                | Base directory for namespace repositories | `/config/`       |
| `VAULT_MASTER_KEY` ⭐             | Master encryption key (base64 encoded)    | Auto-generated   |
| `CONFIG_COMMIT_HISTORY_SIZE`      | Maximum commits returned in history API   | `20`             |
| `VAULT_ENABLED`                   | Enable vault functionality                | `true`           |
| `CACHE_TTL`                       | Global cache time-to-live in seconds      | `600`            |
| `VAULT_MAX_SECRETS_PER_OPERATION` | Maximum secrets per bulk operation        | `100`            |

⭐ **VAULT_MASTER_KEY**: This is the most important security configuration. See the [Vault Security Setup](#vault-security-setup) section below.

### API Access URLs

The application provides two different API interfaces:

1. **Management APIs** (documented in this README):
   - Configuration Management: `http://localhost:8080/config-server/config/*`
   - Namespace Management: `http://localhost:8080/config-server/namespace/*`  
   - Vault Management: `http://localhost:8080/config-server/vault/*`

2. **Spring Cloud Config API** (for client applications):
   - Config Retrieval: `http://localhost:8080/config-server/config-api/{application}/{profile}/{label}`
   - Example: `http://localhost:8080/config-server/config-api/my-app/production/main`

---

## 🔐 Vault Security Setup

### Master Key Configuration

The application uses a **single master key** for encrypting all vault secrets across all namespaces. This key must be configured via the `VAULT_MASTER_KEY` environment variable.

#### Key Generation

Generate a secure 256-bit (32-byte) key and encode it in base64:

```bash
# Generate a new master key
export VAULT_MASTER_KEY=$(openssl rand -base64 32)

# Example output:
# VAULT_MASTER_KEY=8xBqJ9Jk3mN2pQ5rS6tU7vW8xY9zA0bC1dE2fF3gG4hI=
```

#### Development vs Production

**Development (Default Key):**
```bash
# Uses default key from application.yml - NOT SECURE
java -jar config-server.jar

# Logs will show:
# ⚠️ SECURITY WARNING: Using default vault master key from application.yml
# ⚠️ This is NOT secure for production! Set VAULT_MASTER_KEY environment variable.
```

**Production (Custom Key):**
```bash
# Set your own secure key
export VAULT_MASTER_KEY=YourSecureBase64KeyHere
java -jar config-server.jar

# Logs will show:
# ✅ Master encryption key loaded from VAULT_MASTER_KEY environment variable
```

#### Security Best Practices

1. **Never hardcode keys** in source code or configuration files
2. **Use secrets management** (AWS Secrets Manager, Vault, K8s secrets)
3. **Rotate keys periodically** (requires re-encrypting all secrets)
4. **Backup your key securely** (without it, all vault data is lost)
5. **Use different keys** for different environments (dev/staging/prod)

#### Key Rotation Process

⚠️ **Important**: Changing the master key will make all existing vault secrets unreadable!

To rotate the master key:

1. **Backup all vault secrets** (export using `/vault/get` API)
2. **Stop the application**
3. **Set new `VAULT_MASTER_KEY`**
4. **Start the application**
5. **Re-import all secrets** (using `/vault/update` API)

### Docker/Kubernetes Integration


#### Kubernetes Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: config-server-vault-key
type: Opaque
data:
  master-key: <base64-encoded-master-key>
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
spec:
  template:
    spec:
      containers:
      - name: config-server
        image: config-server:latest
        env:
        - name: VAULT_MASTER_KEY
          valueFrom:
            secretKeyRef:
              name: config-server-vault-key
              key: master-key
```

#### AWS ECS with Secrets Manager
```yaml
taskDefinition:
  containerDefinitions:
    - name: config-server
      image: config-server:latest
      secrets:
        - name: VAULT_MASTER_KEY
          valueFrom: arn:aws:secretsmanager:region:account:secret:config-server-vault-key
```

---

## Docker

### Building and Running

```bash
# Build image
docker build -t config-server:latest .

# Run with default key (development only)
docker run --name config-server -p 8080:8080 config-server:latest

# Run with custom master key (production)
docker run --name config-server -p 8080:8080 \
  -e VAULT_MASTER_KEY="$(openssl rand -base64 32)" \
  -v config-data:/config \
  config-server:latest
```

### Production Deployment Checklist

Before deploying to production:

- [ ] ✅ Set custom `VAULT_MASTER_KEY` (never use default)
- [ ] ✅ Configure proper volume mounts for `/config` directory  
- [ ] ✅ Set up log aggregation and monitoring
- [ ] ✅ Configure health checks and restart policies
- [ ] ✅ Use secrets management for environment variables
- [ ] ✅ Set appropriate resource limits (CPU/Memory)
- [ ] ✅ Configure HTTPS/TLS termination (reverse proxy)
- [ ] ✅ Set up backup strategy for configuration data