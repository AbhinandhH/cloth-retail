package com.clothingretail.auth.service;

import com.clothingretail.auth.Role;
import com.clothingretail.auth.RoleName;
import com.clothingretail.auth.User;
import com.clothingretail.auth.repository.RoleRepository;
import com.clothingretail.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On startup, guarantees at least one SUPER_ADMIN account exists. There is
 * deliberately no public admin self-registration endpoint - this bootstrap
 * plus {@code POST /api/admin/admins} (SUPER_ADMIN only) are the only ways
 * an admin account gets created.
 *
 * Credentials come from ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD env
 * vars, falling back to a documented local-dev default - see backend/README.md
 * for why that default must be changed before any real deployment.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-bootstrap.email}") String bootstrapEmail,
            @Value("${app.admin-bootstrap.password}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean superAdminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.hasAnyRole(RoleName.SUPER_ADMIN));
        if (superAdminExists) {
            return;
        }

        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role is not seeded"));

        User user = userRepository.findByEmailIgnoreCase(bootstrapEmail).orElseGet(User::new);
        user.setFullName(user.getFullName() == null ? "Super Admin" : user.getFullName());
        user.setEmail(bootstrapEmail);
        user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        user.setEnabled(true);
        user.getRoles().add(superAdminRole);
        userRepository.save(user);

        log.warn(
                "Bootstrapped SUPER_ADMIN account '{}'. If this is not a local-dev environment, "
                        + "change ADMIN_BOOTSTRAP_PASSWORD / rotate this account's password immediately.",
                bootstrapEmail);
    }
}
