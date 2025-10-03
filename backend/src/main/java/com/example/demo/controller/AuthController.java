package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String username = payload.get("username");

        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email and password are required"));
        }

        if (userService.findByEmail(email) != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(username);
        user.setVerified(false);
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userService.save(user);

        // Send verification email
        String link = "http://localhost:8081/api/auth/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), link);

        return ResponseEntity.ok(Map.of("message", "Account created. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String password = payload.get("password");
        String captcha = payload.get("captcha");

        // Validate captcha
        String expected = (String) session.getAttribute("captcha");
        if (expected == null || captcha == null || !captcha.trim().equalsIgnoreCase(expected)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid captcha"));
        }

        User dbUser = userService.findByEmail(email);
        if (dbUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        if (!dbUser.getPassword().equals(password)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        if (!dbUser.isVerified()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please verify your email before logging in"));
        }

        session.setAttribute("userEmail", dbUser.getEmail());
        // update login timestamps on the user entity
        try {
            dbUser.setPreviousLogin(dbUser.getLastLogin());
            dbUser.setLastLogin(LocalDateTime.now());
            userService.save(dbUser);
        } catch (Exception e) {
            // don't fail login if timestamp save fails
            System.err.println("Failed to update login timestamps: " + e.getMessage());
        }
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        User user = userService.findByVerificationToken(token);
        if (user == null || user.isVerified() || user.getVerificationToken() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "This verification link is expired or already used."));
        }
        user.setVerified(true);
        user.setVerificationToken(null);
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
