# Config Server

## API Endpoints

### Create Configuration

localhost:8080/config-server/config/create

```
{
    "action": "create",
    "appName": "sample",
    "namespace": "default",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Fetch Configuration

localhost:8080/config-server/config/fetch

```
{
    "action": "fetch",
    "appName": "sample",
    "namespace": "default",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Update Configuration

localhost:8080/config-server/config/update

```
{
    "action": "update",
    "appName": "sample",
    "namespace": "default",
    "path": "/",
    "content": "server:\n  port: 8081\n\nspring:\n  application:\n    name:  abc",
    "message": "commit for updating app config",
    "email": "test@gmail.com"
}
```

### Get Configuration History

localhost:8080/config-server/config/history

```
{
    "action": "history",
    "appName": "sample",
    "namespace": "default",
    "path": "/",
    "email": "test@gmail.com"
}
```

### Get Commit Changes

localhost:8080/config-server/config/changes

```
{
    "commitId": "de2c57c02c091da9e61546db416142fe81f84dd3",
    "namespace": "default"
}
```

### Create Namespace

localhost:8080/config-server/config/namespace/create

```
{
    "namespace": "production"
}
```

**Note**: You must create a namespace before creating configuration files in it. This initializes a git repository for the namespace.

## Docker

```bash
# Build image
docker build -t config-server-image .

# Run container
docker run --name config-server -p 8080:8080 config-server-image
```

## Configuration

### Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `SERVER_SERVLET_CONTEXT_PATH` | Application context path | `/config-server` |
| `CONFIG_BASE_PATH` | Base directory for namespace repositories | `/config/` |
| `CONFIG_COMMIT_HISTORY_SIZE` | Maximum commits returned in history API | `10` |