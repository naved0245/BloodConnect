package com.bloodconnect;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Primary admin credentials (admin@bloodconnect.com / Admin@123) are defined in AuthService
        // and bypass MySQL completely without requiring database seeding.
    }
}
