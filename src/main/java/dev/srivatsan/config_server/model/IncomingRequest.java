package dev.srivatsan.config_server.model;

import lombok.Data;

@Data
public class IncomingRequest {

    private String appName; /* application name */
    private String namespace; /* K8s Namespace */
    private String path; /* path inside repository */
    private String content; /* application configuration */
    private String action; /* create, fetch, update */
    private String fileName; /* configuration file name */

    public String getFileName() {
        return this.appName + ".yml";
    }
}
