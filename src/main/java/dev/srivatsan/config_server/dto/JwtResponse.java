package dev.srivatsan.config_server.dto;

import dev.srivatsan.config_server.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private Set<Role> roles;

    public JwtResponse(String accessToken, String username, Set<Role> roles) {
        this.token = accessToken;
        this.username = username;
        this.roles = roles;
    }
}