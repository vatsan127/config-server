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
"appName": "sample",
"namespace": "default",
"path": "/",
"content": "server:\n  port: 8081\n\nspring:\n  application:\n    name: sample",
"message": "commit for updating app config"
}
```

## get commit history

localhost:8080/config-server/config/history

Get commit history for all files:
```
{
"action": "history",
"appName": "sample",
"namespace": "default",
"path": "/"
}
```


## get specific commit details

localhost:8080/config-server/config/commit-details

Get all changes for a specific commit:
```
{
"commitId": "a1b2c3d4e5f6789012345678901234567890abcd"
}
```

Get changes for specific file in a commit:
```
{
"commitId": "a1b2c3d4e5f6789012345678901234567890abcd",
"filePath": "configs/sample-app.yml"
}
```

Returns detailed commit information:
- Full commit details (author, date, full message)
- All file changes with clean diff
- Change type for each file (ADD, MODIFY, DELETE, etc.)

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
