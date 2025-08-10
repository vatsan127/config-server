# URL

## create config

localhost:8080/config-server/config/create

```{
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

# ToDo

## Core

1. Docker/K8s service bean Initialization based on the Containerization Environment
2. logic for different profiles on different path
3. Refresh Config and Restart Application should be added
4. Config history should be added
5. Exception handling for different scenarios
6. Add Logback configuration
7. Add Authentication and authorization
8. ToDO: add aop for incoming request 