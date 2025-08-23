package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Component
public class UtilService {

    private final Logger log = LoggerFactory.getLogger(UtilService.class);

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

    public String getRelativeFilePath(Payload request) {
        return request.getNamespace() + request.getPath() + request.getFileName();
    }

    public void validateActionType(Payload request, ActionType actionType) {
        if (!actionType.equals(request.getAction())) {
            throw new RuntimeException("Invalid action Type for request " + request.getAction());
        }
    }

    public String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}
