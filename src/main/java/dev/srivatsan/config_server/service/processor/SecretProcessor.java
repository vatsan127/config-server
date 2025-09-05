package dev.srivatsan.config_server.service.processor;

import java.util.Map;
import java.util.Set;

public interface SecretProcessor {
    
    String processConfigurationForClient(String configContent, String namespace);
    
    String processConfigurationForInternal(String configContent, String namespace);

}