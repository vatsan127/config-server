package dev.srivatsan.config_server.model;

import lombok.Data;

@Data
public class IncomingRequest {

    private String path;
    private String content;
    private String appName;
    private String fileName;

    public IncomingRequest(String path, String content, String appName, String fileName) {
        this.path = path == null ? "" : path;
        this.content = content;
        this.appName = appName;
        this.fileName = fileName + ".yml";
    }
}
