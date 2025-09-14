package dev.srivatsan.config_server.config;

import dev.srivatsan.config_server.entity.Role;
import dev.srivatsan.config_server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final ApplicationConfig applicationConfig;
    private final Environment environment;

    @Override
    public void run(String... args) {
        initializeDefaultAdminUser();
    }

    private void initializeDefaultAdminUser() {
        String adminUsername = "admin";

        if (userService.findByUsername(adminUsername).isEmpty()) {
            String adminPassword = getAdminPassword();

            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                log.warn("Admin password not configured. Please set 'configserver.admin-password' property or 'ADMIN_PASSWORD' environment variable");
                adminPassword = "admin123"; // Default fallback password
                log.warn("Using default admin password 'admin123'. Please change this in production!");
            }

            try {
                Set<Role> adminRoles = Set.of(Role.ADMIN);
                userService.createUser(adminUsername, adminPassword, adminRoles);
                log.info("Default admin user created successfully with username: {}", adminUsername);
            } catch (Exception e) {
                log.error("Failed to create default admin user", e);
            }
        } else {
            log.info("Admin user already exists, skipping initialization");
        }
    }

    private String getAdminPassword() {
        // Try to get from application config first
        String password = applicationConfig.getAdminPassword();

        // If not found, try environment variable
        if (password == null || password.trim().isEmpty()) {
            password = environment.getProperty("ADMIN_PASSWORD");
        }

        return password;
    }
}