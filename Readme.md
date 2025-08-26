# Config Server

A Git-based Configuration Management Server with multi-namespace support for application configuration files.

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


## Change Log Management

### Get Cached Changes (Fast)

**POST** localhost:8080/config-server/changelog/cached

```json
{
    "namespace": "test"
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
    "path": "config"
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

## Configuration

### Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `SERVER_SERVLET_CONTEXT_PATH` | Application context path | `/config-server` |
| `CONFIG_BASE_PATH` | Base directory for namespace repositories | `/config/` |
| `CONFIG_COMMIT_HISTORY_SIZE` | Maximum commits returned in history API | `10` |