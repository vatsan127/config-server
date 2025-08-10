package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.IncomingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class UtilService {

    private final Logger log = LoggerFactory.getLogger(UtilService.class);
    private final ApplicationConfig applicationConfig;

    public UtilService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public String getYmlFileContent(String absolutePath) throws IOException {
        log.info("Reading and parsing YML file from '{}' into an object", absolutePath);
        return Files.readString(Paths.get(absolutePath));
    }

    public String getYamlValueByKey(String absolutePath, String key) throws IOException {
        Yaml yaml = new Yaml();
        String yamlContent = Files.readString(Paths.get(absolutePath));
        Map<String, Object> yamlMap = yaml.load(yamlContent);
        Object value = yamlMap.get(key);
        log.info("Successfully parsed YML. Found value '{}' for key '{}'", value, key);
        return (value != null) ? value.toString() : null;
    }

    public String getRelativeFilePath(IncomingRequest request) {
        return request.getNamespace() + request.getPath() + request.getFileName();
    }

/*    public String writeToYmlFile(String absolutePath, String content) throws IOException {

    }*/

}
