# Config Server

A Git-based Configuration Management Server with multi-namespace support for application configuration files.

## 🏆 Competitive Comparison

| Feature | **This Config Server** | Spring Cloud Config | HashiCorp Consul | AWS Parameter Store | Azure App Configuration | Kubernetes ConfigMaps |
|---------|------------------------|---------------------|-------------------|---------------------|-------------------------|----------------------|
| **Namespace Isolation** | ✅ **Separate Git repos per namespace** | ⚠️ Branch/label based only | ⚠️ Folder-based separation | ⚠️ Hierarchical paths | ⚠️ App-based grouping | ⚠️ Namespace folders |
| **Built-in Encryption** | ✅ **AES-256-GCM + vault syntax** | ❌ Requires external vault | ✅ Built-in encryption | ✅ KMS encryption | ✅ Built-in encryption | ❌ Base64 encoding only |
| **Version Control** | ✅ **Full Git history per namespace** | ✅ Git-based | ⚠️ Limited KV versioning | ⚠️ Parameter versioning | ⚠️ Simple versioning | ❌ K8s events only |
| **External Dependencies** | ✅ **Zero - single JAR** | ⚠️ Requires Git repo | ❌ Consul cluster required | ❌ AWS services required | ❌ Azure subscription | ❌ Kubernetes cluster |
| **Configuration Size** | ✅ **Unlimited YAML files** | ✅ Unlimited | ✅ Unlimited | ⚠️ 4KB-8KB per parameter | ❌ 10KB per key-value | ⚠️ 1MB limit |
| **Audit Trail** | ✅ **Git commits + email attribution** | ✅ Git commits | ⚠️ Basic logging | ✅ CloudTrail integration | ✅ Activity logs | ⚠️ K8s events |
| **Cost Model** | ✅ **Free (self-hosted)** | ✅ Free (self-hosted) | 💰 Enterprise licensing | 💰 Per-request pricing | 💰 Per-request + storage | ✅ Free (with K8s) |
| **Deployment Model** | ✅ **Self-hosted/Docker** | ✅ Self-hosted | ⚠️ Cluster deployment | ☁️ Managed service only | ☁️ Managed service only | 🏗️ K8s native only |
| **Secret Management** | ✅ **Integrated with YAML substitution** | ❌ External vault required | ✅ Built-in secrets | ✅ Secrets Manager integration | ✅ Key Vault integration | ⚠️ Separate Secrets objects |
| **Real-time Updates** | ⚠️ Pull-based refresh | ⚠️ Pull-based refresh | ✅ Push notifications | ⚠️ Polling required | ✅ Push notifications | ✅ Volume mounts |
| **Multi-Language Support** | ⚠️ REST API (any language) | ⚠️ Spring-focused | ✅ Language agnostic | ✅ SDK for multiple languages | ✅ SDK for multiple languages | ✅ Language agnostic |
| **Conflict Resolution** | ✅ **Optimistic locking** | ❌ Last-write-wins | ❌ Last-write-wins | ❌ Last-write-wins | ❌ Last-write-wins | ❌ Last-write-wins |
| **Notification Tracking** | ✅ **SUCCESS/IN_PROGRESS/FAILED** | ❌ Basic logging | ⚠️ Basic monitoring | ⚠️ CloudWatch integration | ⚠️ Activity logs | ❌ Limited |

### 🎯 **Key Differentiators**

**This Config Server excels at:**
- **🏛️ True namespace isolation** with separate Git repositories
- **🔐 Integrated vault** with **${vault:key}** YAML substitution  
- **📋 Zero dependencies** - single JAR deployment
- **🔄 Git-first approach** with full audit trails
- **⚡ Optimistic locking** prevents configuration conflicts
- **💰 Cost-effective** self-hosted solution

**Choose alternatives for:**
- **Multi-language ecosystems** → Consul or cloud services
- **Managed/serverless requirements** → AWS Parameter Store or Azure App Configuration  
- **Kubernetes-native deployments** → ConfigMaps + External Secrets Operator
- **Real-time push notifications** → Consul or Azure App Configuration
- **Complex service discovery** → HashiCorp Consul

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
├── notify/          # Simple client notification tracking
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

**🔒 Enterprise-Grade Security:**
- **AES-256-GCM encryption** for vault secrets with configurable master keys
- **Optimistic locking** prevents concurrent update conflicts (unique among config servers)
- **Comprehensive input validation** and security checks
- **Email-based audit attribution** for all changes

**🏛️ Advanced Namespace Management:**
- **True isolation** with separate Git repositories per namespace
- **Independent versioning** and rollback capabilities
- **Namespace-specific vault encryption** and secret management
- **Hierarchical directory support** within namespaces

**🔄 Git-First Architecture:**
- **Full Git history** for all configuration changes with detailed commit information
- **Branch and merge support** for configuration management workflows  
- **Distributed version control** benefits (offline capabilities, conflict resolution)
- **Standard Git tooling compatibility** for advanced operations

**⚡ Performance & Reliability:**
- **Intelligent caching** with automatic preloading for faster response times
- **Caffeine cache integration** with configurable TTL settings
- **Async notification processing** using virtual threads
- **Docker-optimized** JVM settings for encryption workloads

**🔐 Integrated Vault System:**
- **YAML-native secret substitution** using **${vault:key}** syntax
- **Zero external dependencies** - no HashiCorp Vault or external secret stores needed
- **Dynamic secret injection** during configuration retrieval
- **Simplified secret management** with merge-based updates

**📊 Operational Excellence:**
- **RESTful API design** with comprehensive request/response models
- **Built-in notification tracking** (SUCCESS/IN_PROGRESS/FAILED status monitoring)
- **Detailed error handling** with structured error responses
- **Production-ready logging** and monitoring capabilities



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

ℹ️ **Note**: The **default** namespace is reserved for system use. Spring Cloud Config uses **main** as the default namespace when no label is specified.

---

# API Endpoints

## 1. Configuration Management API

**Base URL:** **/config**

Manages application configuration files with Git version control and namespace isolation.

### 1.1 Create Configuration File

**Endpoint: POST /config/create**

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

- **action** (string, required): Must be "create"
- **appName** (string, required): Application name (alphanumeric, dash, underscore only)
- **namespace** (string, required): Target namespace (must exist)
- **path** (string, required): Directory path within namespace (usually "/")
- **email** (string, required): Email for Git commit attribution

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

- **201** - Configuration file created successfully
- **400** - Invalid request parameters
- **404** - Namespace not found
- **409** - Configuration file already exists
- **500** - Internal server error

---

### 1.2 Fetch Configuration File

**Endpoint:** **POST /config/fetch**

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

- **action** (string, required): Must be "fetch"
- **appName** (string, required): Application name
- **namespace** (string, required): Source namespace
- **path** (string, required): Directory path within namespace
- **email** (string, required): Email for audit trail

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

- **200** - Configuration file retrieved successfully
- **400** - Invalid request parameters
- **404** - Configuration file or namespace not found
- **500** - Internal server error

---

### 1.3 Update Configuration File

**Endpoint:** **POST /config/update**

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

- **action** (string, required): Must be "update"
- **appName** (string, required): Application name
- **namespace** (string, required): Target namespace
- **path** (string, required): Directory path within namespace
- **content** (string, required): New YAML configuration content
- **message** (string, required): Git commit message
- **email** (string, required): Email for Git commit attribution
- **commitId** (string, optional): Current commit ID for optimistic locking

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

- **200** - Configuration updated successfully
- **400** - Invalid request or YAML content
- **404** - Configuration file not found
- **409** - Commit conflict (file was modified by another user)
- **500** - Internal server error

---

### 1.4 Get Configuration History

**Endpoint:** **POST /config/history**

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

- **200** - History retrieved successfully
- **400** - Invalid request parameters
- **404** - Configuration file not found
- **500** - Internal server error

---

### 1.5 Get Commit Changes

**Endpoint:** **POST /config/changes**

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

- **200** - Commit changes retrieved successfully
- **400** - Invalid commit ID
- **404** - Commit not found
- **500** - Internal server error

---

### 1.6 Delete Configuration File

**Endpoint:** **POST /config/delete**

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

- **200** - Configuration file deleted successfully
- **400** - Invalid request parameters
- **404** - Configuration file not found
- **500** - Internal server error

---

## 2. Namespace Management API

**Base URL:** **/namespace**

Manages configuration namespaces and directory operations with complete isolation.

### 2.1 Create Namespace

**Endpoint:** **POST /namespace/create**

Creates a new namespace directory with Git initialization for configuration isolation.

**Request Model:**

```json
{
  "namespace": "production"
}
```

**Request Fields:**

- **namespace** (string, required): Namespace name (alphanumeric, dash, underscore only)

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

- **201** - Namespace created successfully
- **400** - Invalid namespace name (reserved names: system, admin, dashboard, default, log, root)
- **409** - Namespace already exists
- **500** - Internal server error

⚠️ **Important**: The **default** namespace is reserved for system use. Spring Cloud Config uses **main** as the default namespace when no label is specified.

---

### 2.2 List All Namespaces

**Endpoint:** **POST /namespace/list**

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

- **200** - Namespaces retrieved successfully
- **500** - Internal server error

---

### 2.3 List Directory Contents

**Endpoint:** **POST /namespace/files**

Retrieves the list of .yml files and subdirectories within a specified directory path in a namespace.

**Request Model:**

```json
{
  "namespace": "production",
  "path": "/"
}
```

**Request Fields:**

- **namespace** (string, required): Target namespace
- **path** (string, required): Directory path to list (e.g., "/", "/services", "/config")

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

- **200** - Directory contents retrieved successfully
- **400** - Invalid request parameters
- **404** - Namespace or directory not found
- **500** - Internal server error

---

### 2.4 Delete Namespace

**Endpoint:** **POST /namespace/delete**

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

- **200** - Namespace deleted successfully
- **400** - Invalid namespace name or reserved namespace
- **404** - Namespace not found
- **500** - Internal server error

---

### 2.5 Get Namespace Events

**Endpoint:** **POST /namespace/events**

Retrieves the complete event history (git log) for an entire namespace. Shows all Git commits and activities within the
namespace root directory.

**Request Model:**

```json
{
  "namespace": "production"
}
```

**Request Fields:**

- **namespace** (string, required): Target namespace name

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

- **200** - Namespace events retrieved successfully
- **400** - Invalid request parameters or namespace name
- **404** - Namespace not found
- **500** - Internal server error

---

### 2.6 Get Namespace Notifications

**Endpoint:** **POST /namespace/notify**

Retrieves API call status notifications for the last **commit-history-size** operations within a namespace. Shows
execution status, timing information, retry counts, and results for configuration management operations.

**Request Model:**

```json
{
  "namespace": "production"
}
```

**Request Fields:**

- **namespace** (string, required): Target namespace name

**Response Model:**

```json
{
  "namespace": "production",
  "notifications": [
    {
      "id": "abc123def456789",
      "status": "SUCCESS", 
      "initiatedTime": "2024-01-15T14:35:00"
    },
    {
      "id": "def456abc789012",
      "status": "IN_PROGRESS",
      "initiatedTime": "2024-01-15T14:32:00"
    },
    {
      "id": "ghi789jkl012345", 
      "status": "FAILED",
      "initiatedTime": "2024-01-15T14:25:00"
    }
  ],
  "totalNotifications": 3,
  "maxNotifications": 10
}
```

**Response Fields:**

- **namespace** (string): The namespace that was queried
- **notifications** (array): List of notification status objects
    - **id** (string): Unique identifier (commit ID or generated tracking ID)
    - **status** (string): Current status - **SUCCESS**, **IN_PROGRESS**, or **FAILED**
    - **initiatedTime** (string): When the notification was initiated (ISO format)
- **totalNotifications** (integer): Number of notifications returned
- **maxNotifications** (integer): Maximum notifications limit (from commit-history-size config)

**Status Descriptions:**

- **SUCCESS** - API call completed successfully
- **IN_PROGRESS** - API call is currently being processed
- **FAILED** - API call failed permanently

**Status Codes:**

- **200** - Namespace notifications retrieved successfully
- **400** - Invalid request parameters or namespace name
- **404** - Namespace not found
- **500** - Internal server error

---

## 3. Vault Management API (Simplified)

**Base URL:** **/vault**

🆕 **Simplified Design**: The vault system has been redesigned with just 2 core endpoints for better usability. Secrets
are stored in **.vault/{namespace}-vault.json** files with AES-256-GCM encryption using a single master key.

🔒 **Security**: AES-256-GCM encryption with single master key via environment variable

### 3.1 Get Vault Secrets

**Endpoint:** **POST /vault/get**

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

- **200** - Vault retrieved successfully (returns **{}** if no secrets exist)
- **404** - Namespace not found
- **500** - Internal server error

---

### 3.2 Update Vault Secrets (Complete Replacement)

**Endpoint:** **POST /vault/update**

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

- **200** - Vault updated successfully
- **400** - Invalid request parameters (missing email/commitMessage)
- **404** - Namespace not found
- **500** - Internal server error

---

---

## Cache Management

The application automatically preloads namespaces and directory listings on startup for better performance.

## Notification System

The simplified notification system tracks API call statuses for client refresh operations:

- **In-Memory Storage**: Recent notification statuses are stored in memory (configurable limit)
- **Simple Status Tracking**: Direct status assignment - SUCCESS, IN_PROGRESS, or FAILED
- **Async Processing**: API calls are processed asynchronously using virtual threads
- **Automatic Cleanup**: Old notifications are automatically removed when limits are exceeded

---

## Configuration

### Environment Variables

| Variable                     | Description                               | Default Value    |
|------------------------------|-------------------------------------------|------------------|
| **CONFIG_BASE_PATH**           | Base directory for namespace repositories | **/config/**       |
| **VAULT_MASTER_KEY** ⭐        | Master encryption key (base64 encoded)    | Auto-generated   |
| **CONFIG_COMMIT_HISTORY_SIZE** | Maximum commits returned in history API   | **20**             |
| **VAULT_ENABLED**              | Enable vault functionality                | **true**           |
| **CACHE_TTL**                  | Cache time-to-live in seconds             | **600**            |

⭐ **VAULT_MASTER_KEY**: This is the most important security configuration. See the [Vault Security Setup](#vault-security-setup) section below.

### Fixed Server Configuration

The following server settings are configured in **application.yml** and cannot be overridden via environment variables:
- **HTTP server port**: **8080**
- **Application context path**: **/config-server**
- **Spring Cloud Config API prefix**: **/config-api**

### API Access URLs

The application provides two different API interfaces:

1. **Management APIs** (documented in this README):
   - Configuration Management: **http://localhost:8080/config-server/config/***
   - Namespace Management: **http://localhost:8080/config-server/namespace/***  
   - Vault Management: **http://localhost:8080/config-server/vault/***

2. **Spring Cloud Config API** (for client applications):
   - Config Retrieval: **http://localhost:8080/config-server/config-api/{application}/{profile}/{label}**
   - Example: **http://localhost:8080/config-server/config-api/my-app/production/main**

---

## 🔐 Vault Security Setup

### Master Key Configuration

The application uses a **single master key** for encrypting all vault secrets across all namespaces. This key must be configured via the **VAULT_MASTER_KEY** environment variable.

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
2. **Use secrets management** (HashiCorp Vault, K8s secrets)
3. **Rotate keys periodically** (requires re-encrypting all secrets)
4. **Backup your key securely** (without it, all vault data is lost)
5. **Use different keys** for different environments (dev/staging/prod)

#### Key Rotation Process

⚠️ **Important**: Changing the master key will make all existing vault secrets unreadable!

To rotate the master key:

1. **Backup all vault secrets** (export using **/vault/get** API)
2. **Stop the application**
3. **Set new **VAULT_MASTER_KEY****
4. **Start the application**
5. **Re-import all secrets** (using **/vault/update** API)

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

- [ ] Set custom **VAULT_MASTER_KEY** (never use default)
- [ ] Configure volume mounts for **/config** directory  
- [ ] Set up monitoring and health checks
- [ ] Use secrets management for environment variables
- [ ] Configure HTTPS/TLS termination