package org.dpbe.global.auth.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.dpbe.global.auth.dto.AuthUserResponse;
import org.dpbe.global.auth.dto.CustomerAccountCreateRequest;
import org.dpbe.global.auth.dto.CustomerAccountCreateResponse;
import org.dpbe.global.auth.dto.CustomerSignupRequest;
import org.dpbe.global.auth.dto.CustomerSignupResponse;
import org.dpbe.global.auth.dto.LoginRequest;
import org.dpbe.global.auth.dto.LoginResponse;
import org.dpbe.global.auth.dto.MessageResponse;
import org.dpbe.global.auth.dto.PasswordChangeRequest;
import org.dpbe.global.auth.dto.StaffAccountCreateRequest;
import org.dpbe.global.auth.dto.StaffAccountCreateResponse;
import org.dpbe.global.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpSession session) {
        return authService.login(request, session);
    }

    @PostMapping("/signup/customer")
    public CustomerSignupResponse signupCustomer(@Valid @RequestBody CustomerSignupRequest request) {
        return authService.signupCustomer(request);
    }

    @PostMapping("/customer-accounts")
    public CustomerAccountCreateResponse createCustomerAccount(@RequestBody CustomerAccountCreateRequest request) {
        return authService.createCustomerAccount(request);
    }

    @PostMapping("/staff-accounts")
    public StaffAccountCreateResponse createStaffAccount(@Valid @RequestBody StaffAccountCreateRequest request) {
        return authService.createStaffAccount(request);
    }

    @PostMapping("/password")
    public MessageResponse changePassword(@RequestBody PasswordChangeRequest request, HttpSession session) {
        return authService.changePassword(request, session);
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpSession session) {
        return authService.logout(session);
    }

    @GetMapping("/me")
    public AuthUserResponse me(HttpSession session) {
        return authService.me(session);
    }
}
