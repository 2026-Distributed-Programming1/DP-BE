package org.dpbe.global.auth.service;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.auth.dto.AuthUserResponse;
import org.dpbe.global.auth.dto.AuthenticatedUser;
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
import org.dpbe.global.auth.entity.AuthUser;
import org.dpbe.global.auth.entity.UserRole;
import org.dpbe.global.auth.repository.AuthUserRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String SESSION_USER_KEY = "AUTHENTICATED_USER";
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<UserRole> STAFF_ACCOUNT_ROLES = EnumSet.of(
            UserRole.STAFF,
            UserRole.CONTRACT_STAFF,
            UserRole.CLAIM_STAFF,
            UserRole.UNDERWRITING_STAFF,
            UserRole.SALES_STAFF,
            UserRole.EDUCATION_STAFF,
            UserRole.FINANCE_STAFF,
            UserRole.DISPATCH_STAFF);

    private final AuthUserRepository authUserRepository;
    private final CustomerRepository customerRepository;
    private final AuthAccessService authAccessService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository,
                       CustomerRepository customerRepository,
                       AuthAccessService authAccessService,
                       PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.customerRepository = customerRepository;
        this.authAccessService = authAccessService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        String username = normalize(request.username());
        String password = request.password();
        if (username == null || password == null || password.isBlank()) {
            throw ApiException.badRequest("아이디와 비밀번호를 입력하세요.");
        }

        AuthUser user = authUserRepository.findByUsername(username);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        session.setAttribute(SESSION_USER_KEY, authenticatedUser);
        return new LoginResponse(AuthUserResponse.from(authenticatedUser));
    }

    @Transactional
    public CustomerSignupResponse signupCustomer(CustomerSignupRequest request) {
        String username = normalize(request.username());
        String residentNo = normalizeResidentNo(request.residentNo());
        String phone = normalizePhone(request.phone());
        String email = normalize(request.email());
        if (authUserRepository.existsByUsername(username)) {
            throw ApiException.badRequest("이미 사용 중인 로그인 아이디입니다.");
        }
        if (customerRepository.existsByResidentNo(residentNo)) {
            throw ApiException.badRequest("이미 등록된 주민등록번호입니다.");
        }
        if (customerRepository.existsByPhone(phone)) {
            throw ApiException.badRequest("이미 등록된 연락처입니다.");
        }

        Customer customer = new Customer(null, request.name().trim(), residentNo, phone, email);
        customer.enterAddress(request.address().trim());
        customer.enterBirthDate(request.birthDate());
        Customer savedCustomer = customerRepository.saveNew(customer);

        authUserRepository.save(
                username,
                passwordEncoder.encode(request.password()),
                UserRole.CUSTOMER,
                savedCustomer.getId(),
                savedCustomer.getName(),
                true,
                false);

        return new CustomerSignupResponse(
                savedCustomer.getCustomerId(),
                username,
                "회원가입이 완료되었습니다.");
    }

    @Transactional
    public CustomerAccountCreateResponse createCustomerAccount(CustomerAccountCreateRequest request) {
        authAccessService.requireAdmin();

        String customerId = normalize(request.customerId());
        String username = normalize(request.username());
        if (customerId == null || username == null) {
            throw ApiException.badRequest("고객번호와 로그인 아이디를 입력하세요.");
        }
        if (authUserRepository.existsByUsername(username)) {
            throw ApiException.badRequest("이미 사용 중인 로그인 아이디입니다.");
        }

        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + customerId);
        }
        if (authUserRepository.existsByLinkedCustomerId(customer.getId())) {
            throw ApiException.badRequest("이미 계정이 발급된 고객입니다.");
        }

        String temporaryPassword = generateTemporaryPassword();
        authUserRepository.save(
                username,
                passwordEncoder.encode(temporaryPassword),
                UserRole.CUSTOMER,
                customer.getId(),
                customer.getName(),
                true,
                true);

        AuthUser createdUser = authUserRepository.findByUsername(username);
        return new CustomerAccountCreateResponse(
                AuthUserResponse.from(AuthenticatedUser.from(createdUser)),
                temporaryPassword);
    }

    @Transactional
    public StaffAccountCreateResponse createStaffAccount(StaffAccountCreateRequest request) {
        authAccessService.requireAdmin();

        String username = normalize(request.username());
        String displayName = normalize(request.displayName());
        UserRole role = request.role();
        if (!STAFF_ACCOUNT_ROLES.contains(role)) {
            throw ApiException.badRequest("직원 계정에 사용할 수 없는 역할입니다.");
        }
        if (authUserRepository.existsByUsername(username)) {
            throw ApiException.badRequest("이미 사용 중인 로그인 아이디입니다.");
        }

        String temporaryPassword = generateTemporaryPassword();
        authUserRepository.save(
                username,
                passwordEncoder.encode(temporaryPassword),
                role,
                null,
                displayName,
                true,
                true);

        AuthUser createdUser = authUserRepository.findByUsername(username);
        return new StaffAccountCreateResponse(
                AuthUserResponse.from(AuthenticatedUser.from(createdUser)),
                temporaryPassword);
    }

    @Transactional
    public MessageResponse changePassword(PasswordChangeRequest request, HttpSession session) {
        AuthenticatedUser sessionUser = getAuthenticatedUser(session);
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw ApiException.badRequest("현재 비밀번호와 새 비밀번호를 입력하세요.");
        }
        if (newPassword.length() < 8) {
            throw ApiException.badRequest("새 비밀번호는 8자 이상이어야 합니다.");
        }

        AuthUser user = authUserRepository.findByUsername(sessionUser.username());
        if (user == null || !user.isEnabled()) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        authUserRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword), false);
        AuthUser updatedUser = authUserRepository.findByUsername(user.getUsername());
        session.setAttribute(SESSION_USER_KEY, AuthenticatedUser.from(updatedUser));
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    public AuthUserResponse me(HttpSession session) {
        return AuthUserResponse.from(getAuthenticatedUser(session));
    }

    public MessageResponse logout(HttpSession session) {
        session.invalidate();
        return new MessageResponse("로그아웃되었습니다.");
    }

    public static AuthenticatedUser getAuthenticatedUser(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER_KEY);
        if (user instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw ApiException.unauthorized("로그인이 필요합니다.");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeResidentNo(String value) {
        return normalize(value).replace("-", "");
    }

    private String normalizePhone(String value) {
        String normalized = normalize(value).replace("-", "");
        return normalized.substring(0, 3) + "-" + normalized.substring(3, 7) + "-" + normalized.substring(7);
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
