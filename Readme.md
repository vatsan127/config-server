# URL

## create config

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

## fetch config

localhost:8080/config-server/config/fetch

json

```
{
    "action": "fetch",
    "appName": "sample",
    "namespace": "default",
    "path": "/",
    "email": "test@gmail.com"
}
```

## update config

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

## get history

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

## get changes

localhost:8080/config-server/config/changes

```
{
    "commitId": "de2c57c02c091da9e61546db416142fe81f84dd3"
}
```

## Docker Commands

```
docker build -t config-server-image .
```

```
docker run --name config-server config-server-image
```

# ToDo

## Core

1. Docker/K8s service bean Initialization based on the Containerization Environment
2. logic for different profiles on different path
3. Refresh Config and Restart Application should be added
4. Exception handling for different scenarios
5. Add Logback configuration
6. Add Authentication and authorization
7. Error and Exception handling
8. add swagger 
