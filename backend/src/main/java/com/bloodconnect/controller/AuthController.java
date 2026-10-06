package com.bloodconnect.controller;

import com.bloodconnect.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            Map<String, Object> user = authService.login(email, password);
            return ResponseEntity.ok(Map.of("success", true, "user", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/register-customer")
    public ResponseEntity<?> registerCustomer(@RequestBody Map<String, Object> body) {
        try {
            authService.registerCustomer(body);
            return ResponseEntity.ok(Map.of("success", true, "message", "Donor registration successful! You can now log in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/register-hospital")
    public ResponseEntity<?> registerHospital(@RequestBody Map<String, Object> body) {
        try {
            authService.registerHospital(body);
            return ResponseEntity.ok(Map.of("success", true, "message", "Hospital registered! Account is pending admin verification."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
