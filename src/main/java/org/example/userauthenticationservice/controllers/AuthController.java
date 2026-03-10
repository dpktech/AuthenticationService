package org.example.userauthenticationservice.controllers;

import org.antlr.v4.runtime.misc.Pair;
import org.example.userauthenticationservice.dtos.*;
import org.example.userauthenticationservice.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * FR-1.1: User registration with email and password.
     * Publishes 'signup' Kafka event for welcome email.
     */
    @PostMapping("/sign_up")
    public ResponseEntity<SignUpResponseDto> signUp(@RequestBody SignUpRequestDto request) {
        SignUpResponseDto response = new SignUpResponseDto();
        try {
            if (authService.signUp(request.getEmail(), request.getPassword())) {
                response.setRequestStatus(RequestStatus.SUCCESS);
            } else {
                response.setRequestStatus(RequestStatus.FAILURE);
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setRequestStatus(RequestStatus.FAILURE);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
    }

    /**
     * FR-1.2: Secure login with JWT session management.
     * Returns JWT token in Set-Cookie header.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = new LoginResponseDto();
        try {
            Pair<Boolean, String> tokenWithResult = authService.login(request.getEmail(), request.getPassword());
            response.setRequestStatus(RequestStatus.SUCCESS);
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.SET_COOKIE, tokenWithResult.b);
            return new ResponseEntity<>(response, headers, HttpStatus.OK);
        } catch (Exception e) {
            response.setRequestStatus(RequestStatus.FAILURE);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * FR-6.2: Logout - marks session as EXPIRED in MySQL.
     */
    @PostMapping("/logout")
    public ResponseEntity<SignUpResponseDto> logout(@RequestBody LogoutRequestDto request) {
        SignUpResponseDto response = new SignUpResponseDto();
        try {
            boolean result = authService.logout(request.getUserId(), request.getToken());
            response.setRequestStatus(result ? RequestStatus.SUCCESS : RequestStatus.FAILURE);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setRequestStatus(RequestStatus.FAILURE);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * FR-6.1: Token validation for protected endpoints.
     */
    @PostMapping("/validateToken")
    public Boolean validateToken(@RequestBody ValidateTokenDto validateTokenDto) {
        return authService.validateToken(validateTokenDto.getUserId(), validateTokenDto.getToken());
    }

    /**
     * FR-1.4: Initiate password reset - sends reset link via Kafka → EmailService.
     */
    @PostMapping("/password-reset")
    public ResponseEntity<SignUpResponseDto> initiatePasswordReset(@RequestBody PasswordResetRequestDto request) {
        SignUpResponseDto response = new SignUpResponseDto();
        try {
            authService.initiatePasswordReset(request.getEmail());
            response.setRequestStatus(RequestStatus.SUCCESS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.setRequestStatus(RequestStatus.FAILURE);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
}
