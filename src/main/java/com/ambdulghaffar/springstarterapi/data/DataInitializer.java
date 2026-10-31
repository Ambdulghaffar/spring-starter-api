package com.ambdulghaffar.springstarterapi.data;

import com.ambdulghaffar.springstarterapi.entity.*;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ambdulghaffar.springstarterapi.entity.enums.UserEnum.UserRole.ADMIN;
import static com.ambdulghaffar.springstarterapi.entity.enums.UserEnum.UserRole.USER;
import static com.ambdulghaffar.springstarterapi.entity.enums.UserEnum.UserStatus.ACTIVE;

@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized, skipping.");
            return;
        }

        var users = createUsers();
        log.info("Database initialization completed.");
    }

    private List<User> createUsers() {
        var adminPassword = passwordEncoder.encode("admin");
        var userPassword = passwordEncoder.encode("visitor");

        var users = List.of(
                user("Admin", "admin", "admin@gmail.com",
                        "+212600000001", adminPassword, ADMIN, ACTIVE),

                user("Visitor", "visit", "visitor@gmail.com",
                        "+212600000002", userPassword, USER, ACTIVE),

                user("John", "Doe", "john.doe@gmail.com",
                        "+212600000033", userPassword, USER, ACTIVE),

                user("Jane", "Smith", "jane.smith@gmail.com",
                        "+212600000003", userPassword, USER, ACTIVE),

                user("Eric", "Dupont", "eric.dupont@gmail.com",
                        "+212600000004", userPassword, USER, ACTIVE),

                user("Alice", "Johnson", "alice.johnson@gmail.com",
                        "+212600000005", userPassword, USER, ACTIVE),

                user("Michael", "Brown", "michael.brown@gmail.com",
                        "+212600000006", userPassword, USER, ACTIVE),

                user("Emma", "Wilson", "emma.wilson@gmail.com",
                        "+212600000007", userPassword, USER, ACTIVE),

                user("David", "Miller", "david.miller@gmail.com",
                        "+212600000008", userPassword, USER, ACTIVE),

                user("Olivia", "Moore", "olivia.moore@gmail.com",
                        "+212600000009", userPassword, USER, ACTIVE),

                user("Daniel", "Taylor", "daniel.taylor@gmail.com",
                        "+212600000010", userPassword, USER, ACTIVE),

                user("Sophia", "Anderson", "sophia.anderson@gmail.com",
                        "+212600000011", userPassword, USER, ACTIVE),

                user("James", "Thomas", "james.thomas@gmail.com",
                        "+212600000012", userPassword, USER, ACTIVE),

                user("Charlotte", "Jackson", "charlotte.jackson@gmail.com",
                        "+212600000013", userPassword, USER, ACTIVE),

                user("Lucas", "White", "lucas.white@gmail.com",
                        "+212600000014", userPassword, USER, ACTIVE),

                user("Amelia", "Harris", "amelia.harris@gmail.com",
                        "+212600000015", userPassword, USER, ACTIVE)
        );

        return userRepository.saveAll(users);
    }

    private User user(
            String firstName,
            String lastName,
            String email,
            String phone,
            String password,
            UserEnum.UserRole role,
            UserEnum.UserStatus status
    ) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phone)
                .password(password)
                .role(role)
                .status(status)
                .build();
    }
}