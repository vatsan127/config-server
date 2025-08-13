package dev.srivatsan.config_server.model;

import lombok.Data;

@Data
public class Payload {

    private String appName; /* application name */
    private String namespace; /* K8s Namespace */
    private String path; /* path inside repository */
    private String content; /* application configuration */
    private ActionType action; /* ActionType - create, fetch, update */
    private String fileName; /* configuration file name */

    public String getFileName() {
        return this.appName + ".yml";
    }
}
