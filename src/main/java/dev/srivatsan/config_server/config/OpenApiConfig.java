package dev.srivatsan.config_server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Config Server API")
                        .description("Git-based Configuration Management Server with multi-namespace support")
                        .version("1.0.0"));
    }
}