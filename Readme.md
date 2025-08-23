# URL

## create config

localhost:8080/config-server/config/create

```
{
    "action": "create",
    "appName": "sample",
    "namespace": "default",
    "path": "/"
}
```

## fetch config

localhost:8080/config-server/config/fetch

json

```
{
    "action": "fetch",
    "appName": "sample",
    "namespace": "default",
    "path": "/"
}
```

## update config

localhost:8080/config-server/config/update

```
{
    "action": "update",
    "appName": "test",
    "namespace": "default",
    "path": "/",
    "content": "server:\n  port: 8081\n\nspring:\n  application:\n    name: abc",
    "message": "commit for updating app config"
}
```

## get history

localhost:8080/config-server/config/history

```
{
    "action": "history",
    "appName": "test",
    "namespace": "default",
    "path": "/"
}
```

## get changes

localhost:8080/config-server/config/changes

```
{
    "commitId": "de2c57c02c091da9e61546db416142fe81f84dd3"
}
```

# ToDo

## Core

1. Docker/K8s service bean Initialization based on the Containerization Environment
2. logic for different profiles on different path
3. Refresh Config and Restart Application should be added
4. Exception handling for different scenarios
5. Add Logback configuration
6. Add Authentication and authorization
7. Send payload after the initializing a config
8. Error and Exception handling
9. Create endpoint for update and commit
10. Config history should be added
