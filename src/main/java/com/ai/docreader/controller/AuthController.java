package com.ai.docreader.controller;

import com.ai.docreader.dto.AuthDtos.AuthResponse;
import com.ai.docreader.dto.AuthDtos.LoginRequest;
import com.ai.docreader.dto.AuthDtos.RegisterRequest;
import com.ai.docreader.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Public auth endpoints — no JWT required.
 *
 * POST /api/auth/register  →  { name, email, password } → { token, email, name }
 * POST /api/auth/login     →  { email, password }       → { token, email, name }
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            if (request.name()     == null || request.name().isBlank() ||
                request.email()    == null || request.email().isBlank() ||
                request.password() == null || request.password().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Name, email, and password (min 6 chars) are required."));
            }
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid email or password."));
        }
    }
}