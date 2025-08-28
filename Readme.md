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

Interactive API documentation is available via Swagger UI:

- **Swagger UI**: `http://localhost:8080/config-server/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/config-server/v3/api-docs`

The Swagger interface provides complete API documentation with request/response examples, parameter descriptions, and the ability to test endpoints directly.

> **✨ Enhanced Documentation**: All endpoints now include comprehensive Swagger annotations with examples, validation details, and proper error responses.

## API Endpoints

### Create Configuration

**POST** localhost:8080/config-server/config/create

```json
{
    "action": "create",
    "appName": "sample",
    "namespace": "test",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Fetch Configuration

**POST** localhost:8080/config-server/config/fetch

```json
{
    "action": "fetch",
    "appName": "sample",
    "namespace": "test",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Update Configuration

**POST** localhost:8080/config-server/config/update

```json
{
    "action": "update",
    "appName": "sample",
    "namespace": "test",
    "path": "/",
    "content": "server:\n  port: 8081\n\nspring:\n  application:\n    name:  abc",
    "message": "commit for updating app config",
    "email": "test@gmail.com"
}
```

### Get Configuration History

**POST** localhost:8080/config-server/config/history

```json
{
    "action": "history",
    "appName": "sample",
    "namespace": "test",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Get Commit Changes

**POST** localhost:8080/config-server/config/changes

```json
{
    "action": "changes",
    "appName": "sample",
    "namespace": "test",
    "path": "/",
    "email": "test@gmail.com",
    "commitId": "de2c57c02c091da9e61546db416142fe81f84dd3"
}
```


## Namespace Management

### Create Namespace

**POST** localhost:8080/config-server/namespace/create

```json
{
    "namespace": "test"
}
```

**Note**: You must create a namespace before creating configuration files in it. This initializes a git repository for the namespace.

### List Namespaces

**POST** localhost:8080/config-server/namespace/list

```json
{}
```

### List Directory Contents

**POST** localhost:8080/config-server/namespace/files

```json
{
    "namespace": "test",
    "path": "/"
}
```

## Docker

```bash
# Build image
docker build -t config-server-image .

# Run container
docker run --name config-server -p 8080:8080 config-server-image
```

## 🚀 Getting Started

1. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Access Swagger UI**: Navigate to `http://localhost:8080/config-server/swagger-ui/index.html`

3. **Create a namespace** (required before creating config files):
   ```bash
   curl -X POST http://localhost:8080/config-server/namespace/create \
     -H "Content-Type: application/json" \
     -d '{"namespace": "test"}'
   ```

4. **Create your first configuration file** using the Swagger UI or API endpoints below.

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