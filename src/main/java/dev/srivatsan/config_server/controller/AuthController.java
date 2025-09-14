package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.dto.JwtResponse;
import dev.srivatsan.config_server.dto.LoginRequest;
import dev.srivatsan.config_server.entity.User;
import dev.srivatsan.config_server.security.JwtTokenProvider;
import dev.srivatsan.config_server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            User user = (User) authentication.getPrincipal();

            return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getRoles()));
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body("Error: Invalid username or password!");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("User not authenticated");
        }

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new JwtResponse(null, user.getUsername(), user.getRoles()));
    }
}