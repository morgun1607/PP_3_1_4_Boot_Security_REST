package ru.kata.spring.boot_security.demo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kata.spring.boot_security.demo.repository.UserRepository;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Collections;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeUsers(RoleRepository roles, UserRepository users, PasswordEncoder encoder,
            TransactionTemplate tx,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.user.password:}") String userPassword) {
        return args -> tx.executeWithoutResult(status -> {
            Role adminRole = roles.findByName("ROLE_ADMIN").orElseGet(() -> roles.save(new Role("ROLE_ADMIN")));
            Role userRole = roles.findByName("ROLE_USER").orElseGet(() -> roles.save(new Role("ROLE_USER")));
            if (!adminPassword.isBlank() && users.findByEmail("admin@example.com").isEmpty()) {
                User admin = new User("Admin", "Account", "admin@example.com", encoder.encode(adminPassword));
                admin.setAge(35);
                admin.setRoles(new java.util.HashSet<>(java.util.Arrays.asList(adminRole, userRole)));
                users.save(admin);
            }
            if (!userPassword.isBlank() && users.findByEmail("user@example.com").isEmpty()) {
                User user = new User("User", "Account", "user@example.com", encoder.encode(userPassword));
                user.setAge(30);
                user.setRoles(Collections.singleton(userRole));
                users.save(user);
            }
        });
    }
}
