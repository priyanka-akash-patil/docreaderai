package com.ai.docreader.dto;

/**
 * Data Transfer Objects for authentication endpoints.
 * Records = immutable, no boilerplate, perfect for request/response bodies.
 */
public class AuthDtos {

    // POST /api/auth/register  →  request body
    public record RegisterRequest(
            String name,
            String email,
            String password
    ) {}

    // POST /api/auth/login  →  request body
    public record LoginRequest(
            String email,
            String password
    ) {}

    // Both register and login return this
    public record AuthResponse(
            String token,
            String email,
            String name,
            String message
    ) {}
}
