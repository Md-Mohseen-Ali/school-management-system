package com.schoolmanagement.security.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.UserRole;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;

/**
 * Enables the seeded assessment administrator only when the operator supplies
 * a private password through the backend environment. The fixed credential
 * from the original seed is disabled by Flyway V8.
 */
@Component
public class DemoAdminPasswordBootstrap implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@school.demo";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredPassword;

    public DemoAdminPasswordBootstrap(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.admin-password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredPassword = configuredPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configuredPassword == null || configuredPassword.isBlank()) {
            // Leave the fixed demo administrator disabled when no private
            // operator-managed password has been configured.
            return;
        }

        int passwordBytes = configuredPassword.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 16 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "DEMO_ADMIN_PASSWORD must contain 16 to 72 UTF-8 bytes");
        }

        AppUser admin = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                .orElseThrow(() -> new IllegalStateException(
                        "Seeded assessment administrator was not found"));

        if (!admin.getRoles().contains(UserRole.ADMIN)) {
            throw new IllegalStateException(
                    "The seeded assessment account does not have the ADMIN role");
        }

        admin.setPasswordHash(passwordEncoder.encode(configuredPassword));
        admin.setEnabled(true);
        userRepository.saveAndFlush(admin);
    }
}
