package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final MailService mailService;

    public AdminUserService(UserRepository userRepository, PasswordService passwordService, MailService mailService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.mailService = mailService;
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUser(Integer userId) {
        return userRepository.findById(userId);
    }

    public User resetPassword(User user) {
        String temporaryPassword = passwordService.generateTemporaryPassword();
        user.setPassword(passwordService.encode(temporaryPassword));
        user.setTemporaryPassword(true);
        user.setLastPasswordChange(LocalDateTime.now());
        user.setLoginFailCount(0);
        user.setStatus(0);
        User saved = userRepository.save(user);
        mailService.sendTemporaryPassword(saved.getEmail(), saved.getName(), temporaryPassword);
        return saved;
    }
}
