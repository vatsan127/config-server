package dev.srivatsan.config_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.yaml.snakeyaml.Yaml;

@Data
@AllArgsConstructor
public class YamlConfigResponse {
    private String applicationName;
    private String namespace;
    private String path;
    private String yamlContent;
}
