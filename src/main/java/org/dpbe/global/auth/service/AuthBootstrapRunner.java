package org.dpbe.global.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.dpbe.global.auth.entity.UserRole;
import org.dpbe.global.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.auth.bootstrap.enabled", havingValue = "true")
public class AuthBootstrapRunner implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AuthBootstrapRunner(AuthUserRepository authUserRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.auth.bootstrap.username}") String username,
                               @Value("${app.auth.bootstrap.password}") String password,
                               @Value("${app.auth.bootstrap.display-name}") String displayName) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(String... args) {
        if (password == null || password.isBlank()) {
            log.warn("[auth-bootstrap] APP_AUTH_BOOTSTRAP_PASSWORD is blank. Skipping admin user creation.");
            return;
        }
        if (authUserRepository.existsByUsername(username)) {
            log.info("[auth-bootstrap] auth user '{}' already exists. Skipping.", username);
            return;
        }
        authUserRepository.save(
                username,
                passwordEncoder.encode(password),
                UserRole.ADMIN,
                null,
                displayName,
                true);
        log.info("[auth-bootstrap] admin auth user '{}' created.", username);
    }
}
