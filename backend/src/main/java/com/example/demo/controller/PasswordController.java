package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:8081")  // Allow requests from your frontend origin
@RestController
@RequestMapping("/api/password")
public class PasswordController {
    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;

    @PostMapping("/forgot")
    public String forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String recaptcha = payload.get("recaptcha");

        // 1. Verify reCAPTCHA
        if (recaptcha == null || recaptcha.isEmpty()) {
            return "Captcha required";
        }

        String secret = "6Ldfo9orAAAAAI6mNCznrLCf01O6IZQajVpQprwl"; // Google test secret key
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "secret=" + secret + "&response=" + recaptcha;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<HashMap<String, Object>> recaptchaResponse = restTemplate.postForEntity(
            verifyUrl, entity, (Class<HashMap<String, Object>>) (Class<?>) HashMap.class);

        Map<String, Object> responseBody = recaptchaResponse.getBody();

        if (responseBody == null || !(Boolean) responseBody.getOrDefault("success", false)) {
            return "Captcha verification failed";
        }

        // 2. Proceed with forgot password
        User dbUser = userService.findByEmail(email);
        if (dbUser == null) {
            return "Email not found";
        }

        String token = UUID.randomUUID().toString();
        dbUser.setResetToken(token);
        userService.save(dbUser);

        emailService.sendResetEmail(dbUser.getEmail(), token);

        return "Verification email sent";
    }

    @PostMapping("/reset")
    public String resetPassword(@RequestBody ResetRequest request) {
        User dbUser = userService.findByResetToken(request.getToken());
        if (dbUser == null) {
            return "Invalid token";
        }

        dbUser.setPassword(request.getNewPassword());
        dbUser.setResetToken(null);
        userService.save(dbUser);

        return "Password reset successful";
    }

    public static class ResetRequest {
        private String token;
        private String newPassword;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
