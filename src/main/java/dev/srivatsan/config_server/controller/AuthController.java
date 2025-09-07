package dev.srivatsan.config_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> getLoginPage(@RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            return ResponseEntity.status(401).body(Map.of(
                    "message", "Login failed",
                    "error", "Invalid username or password"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "message", "Please provide your credentials",
                "loginUrl", "/login"
        ));
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of(
                    "username", auth.getName(),
                    "authorities", auth.getAuthorities(),
                    "authenticated", true
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of(
                "authenticated", false,
                "message", "Not authenticated"
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "username", auth.getName(),
                    "authorities", auth.getAuthorities(),
                    "message", "Session is valid"
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of(
                "authenticated", false,
                "message", "Session expired or not authenticated"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCredentials(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        if (username == null || password == null) {
            return ResponseEntity.status(400).body(Map.of(
                    "valid", false,
                    "message", "Username and password are required"
            ));
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName().equals(username)) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username,
                    "message", "Credentials are valid"
            ));
        }
        
        return ResponseEntity.status(401).body(Map.of(
                "valid", false,
                "message", "Invalid credentials or not authenticated"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}