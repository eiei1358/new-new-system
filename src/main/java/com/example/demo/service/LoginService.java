package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public LoginService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim());
    }

    public boolean passwordMatches(String rawPassword, User user) {
        return user != null && passwordService.matches(rawPassword, user.getPassword());
    }

    public void recordLoginSuccess(User user) {
        user.setLoginFailCount(0);
        user.setLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void recordLoginFailure(User user) {
        int failCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
        user.setLoginFailCount(failCount + 1);
        if (user.getLoginFailCount() >= 3) {
            user.setStatus(1);
        }
        userRepository.save(user);
    }
}
