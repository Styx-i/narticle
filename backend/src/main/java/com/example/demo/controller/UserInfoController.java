package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserInfoController {
    @Autowired
    private UserService userService;

@GetMapping("/me")
public User getCurrentUser(HttpSession session) {
    String email = (String) session.getAttribute("userEmail");
    if (email == null) return null;
    User user = userService.findByEmail(email);
    if (user == null) return null;
    // Return both name and email
    User result = new User();
    result.setName(user.getName());
    result.setEmail(user.getEmail());
    result.setLastLogin(user.getLastLogin());
    result.setPreviousLogin(user.getPreviousLogin());
    return result;
}
}
