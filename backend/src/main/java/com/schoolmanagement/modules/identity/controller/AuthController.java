package com.schoolmanagement.modules.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import com.schoolmanagement.modules.identity.dto.AuthResponse;
import com.schoolmanagement.modules.identity.dto.LoginRequest;
import com.schoolmanagement.security.jwt.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        try {

            var authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.email().trim(),
                                    request.password()
                            )
                    );

            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();

            String token =
                    jwtService.generateAccessToken(userDetails);

            return new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getAccessTokenValiditySeconds()
            );

        } catch (AuthenticationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
    }
    
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {

        return Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList(),
                "authenticated", authentication.isAuthenticated()
        );
    }
}