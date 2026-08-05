package com.atwo.currencies_api.currency.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atwo.currencies_api.currency.dtos.LoginRequest;
import com.atwo.currencies_api.currency.dtos.LoginResponse;
import com.atwo.currencies_api.currency.security.JwtService;

@RestController
@RequestMapping("/api/admin")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminPasswordHash;

    public AuthController(PasswordEncoder passwordEncoder, JwtService jwtService,
            @Value("${admin.password-hash:}") String adminPasswordHash) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminPasswordHash = adminPasswordHash;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        boolean matches;
        try {
            matches = passwordEncoder.matches(request.senha(), adminPasswordHash);
        } catch (IllegalArgumentException e) {
            matches = false;
        }
        if (!matches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken()));
    }
}
