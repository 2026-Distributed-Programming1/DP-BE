package org.dpbe.global.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.entity.UserRole;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuthAccessService {

    public AuthenticatedUser currentUser() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw ApiException.unauthorized("로그인이 필요합니다.");
        }
        return AuthService.getAuthenticatedUser(session);
    }

    public boolean isCustomer() {
        return currentUser().role() == UserRole.CUSTOMER;
    }

    public void requireAdmin() {
        if (currentUser().role() != UserRole.ADMIN) {
            throw ApiException.forbidden("관리자만 수행할 수 있습니다.");
        }
    }

    public void requireCustomerAccess(Customer customer) {
        if (customer == null || !isCustomer()) {
            return;
        }
        requireCustomerNoAccess(customer.getCustomerId());
    }

    public void requireCustomerNoAccess(String customerNo) {
        AuthenticatedUser user = currentUser();
        if (user.role() != UserRole.CUSTOMER) {
            return;
        }
        if (user.linkedCustomerNo() == null || !user.linkedCustomerNo().equals(customerNo)) {
            throw ApiException.forbidden("본인 고객 데이터만 접근할 수 있습니다.");
        }
    }

    public void requireContractAccess(Contract contract) {
        if (contract == null || contract.getCustomer() == null || !isCustomer()) {
            return;
        }
        requireCustomerNoAccess(contract.getCustomer().getCustomerId());
    }

    public boolean canAccessContract(Contract contract) {
        AuthenticatedUser user = currentUser();
        if (user.role() != UserRole.CUSTOMER) {
            return true;
        }
        return contract != null
                && contract.getCustomer() != null
                && user.linkedCustomerNo() != null
                && user.linkedCustomerNo().equals(contract.getCustomer().getCustomerId());
    }
}
