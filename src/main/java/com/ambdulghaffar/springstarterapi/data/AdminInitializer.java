package com.ambdulghaffar.springstarterapi.data;

import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Optional;

@Slf4j
@Profile("!local & session")
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        Assert.hasText(adminEmail, "ADMIN_EMAIL must not be empty");
        Assert.hasText(adminPassword, "ADMIN_PASSWORD must not be empty");

        Optional<User> bootstrapAdmin = userRepository.findFirstByEnvManagedTrueAndDeletedAtIsNull();

        if (bootstrapAdmin.isEmpty()) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .firstName("Admin")
                    .lastName("Admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserEnum.UserRole.ADMIN)
                    .status(UserEnum.UserStatus.ACTIVE)
                    .envManaged(true)
                    .build());

            log.info("Bootstrap admin account created.");
            return;
        }

        User admin = bootstrapAdmin.get();
        boolean changed = false;

        if (!admin.getEmail().equals(adminEmail)) {
            admin.setEmail(adminEmail);
            changed = true;
        }

        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }

        if (changed) {
            userRepository.save(admin);
            log.info("Bootstrap admin synced with environment variables.");
        } else {
            log.info("Bootstrap admin already up to date, skipping.");
        }
    }
}