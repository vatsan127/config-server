package dev.srivatsan.config_server.dto;

import dev.srivatsan.config_server.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotEmpty(message = "User must have at least one role")
    private Set<Role> roles;
}