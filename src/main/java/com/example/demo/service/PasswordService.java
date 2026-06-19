package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    public String encode(String rawPassword) {
        return PasswordUtil.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        return PasswordUtil.matches(rawPassword, storedPassword);
    }

    public String generateTemporaryPassword() {
        return PasswordUtil.generateTemporaryPassword();
    }
}
