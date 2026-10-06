package com.bloodconnect;

import com.bloodconnect.model.User;
import com.bloodconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@bloodconnect.com")) {
            User admin = new User(
                "admin@bloodconnect.com",
                passwordEncoder.encode("Admin@123"),
                "System Administrator",
                "9876543210",
                "ADMIN",
                true
            );
            userRepository.save(admin);
            System.out.println(">>> Seeded default admin account: admin@bloodconnect.com / Admin@123");
        }
    }
}
