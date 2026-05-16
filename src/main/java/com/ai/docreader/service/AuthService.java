package com.ai.docreader.service;

import com.ai.docreader.dto.AuthDtos.AuthResponse;
import com.ai.docreader.dto.AuthDtos.LoginRequest;
import com.ai.docreader.dto.AuthDtos.RegisterRequest;
import com.ai.docreader.entity.User;
import com.ai.docreader.repo.UserRepository;
import com.ai.docreader.service.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Register:  validate → BCrypt hash → save to DB → generate JWT
 * Login:     Spring Security verifies credentials → generate JWT
 */
@Service
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService    userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository        = userRepository;
        this.passwordEncoder       = passwordEncoder;
        this.jwtService            = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService    = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.email());
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name()
        );
        userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, user.getEmail(), user.getName(),
                "Welcome, " + user.getName() + "! Registration successful.");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return new AuthResponse(token, user.getEmail(), user.getName(),
                "Welcome back, " + user.getName() + "!");
    }
}