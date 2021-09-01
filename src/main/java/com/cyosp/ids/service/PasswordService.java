package com.cyosp.ids.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    public String encode(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }
}

