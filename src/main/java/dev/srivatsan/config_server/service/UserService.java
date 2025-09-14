package dev.srivatsan.config_server.service;

import dev.srivatsan.config_server.entity.Role;
import dev.srivatsan.config_server.entity.User;
import dev.srivatsan.config_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user;
    }

    @Transactional
    public User createUser(String username, String password, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User must have at least one role");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new HashSet<>(roles)); // Create defensive copy
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("Created new user: {} with roles: {}", username, roles);
        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Deleted user with id: {}", userId);
    }

    @Transactional
    public User updateUser(Long userId, String username, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User must have at least one role");
        }

        user.setUsername(username);
        user.setRoles(new HashSet<>(roles)); // Create defensive copy

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {} with roles: {}", username, roles);
        return updatedUser;
    }
}