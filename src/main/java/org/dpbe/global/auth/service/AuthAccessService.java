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

    public void requireStaffOrAdmin() {
        UserRole role = currentUser().role();
        if (role != UserRole.ADMIN && !role.isStaffLike()) {
            throw ApiException.forbidden("직원 또는 관리자만 수행할 수 있습니다.");
        }
    }

    public void requireAnyRole(UserRole... allowedRoles) {
        UserRole currentRole = currentUser().role();
        for (UserRole allowedRole : allowedRoles) {
            if (currentRole == allowedRole) {
                return;
            }
        }
        throw ApiException.forbidden("요청한 작업을 수행할 권한이 없습니다.");
    }

    public void requireFinanceStaff() {
        requireAnyRole(UserRole.FINANCE_STAFF, UserRole.ADMIN);
    }

    public void requireClaimStaff() {
        requireAnyRole(UserRole.CLAIM_STAFF, UserRole.ADMIN);
    }

    public void requireDispatchStaff() {
        requireAnyRole(UserRole.DISPATCH_STAFF, UserRole.CLAIM_STAFF, UserRole.ADMIN);
    }

    public void requireSalesStaff() {
        requireAnyRole(UserRole.SALES_STAFF, UserRole.ADMIN);
    }

    public void requireEducationStaff() {
        requireAnyRole(UserRole.EDUCATION_STAFF, UserRole.ADMIN);
    }

    public void requireUnderwritingStaff() {
        requireAnyRole(UserRole.UNDERWRITING_STAFF, UserRole.ADMIN);
    }

    public void requireContractStaff() {
        requireAnyRole(UserRole.CONTRACT_STAFF, UserRole.ADMIN);
    }

    public void requirePaymentRecordManageAccess() {
        requireFinanceStaff();
    }

    public void requireRefundOperationAccess() {
        requireFinanceStaff();
    }

    public void requireClaimInvestigationAccess() {
        requireClaimStaff();
    }

    public void requireClaimCalculationAccess() {
        requireClaimStaff();
    }

    public void requireClaimPaymentAccess() {
        requireAnyRole(UserRole.FINANCE_STAFF, UserRole.CLAIM_STAFF, UserRole.ADMIN);
    }

    public void requireDispatchRecordAccess() {
        requireDispatchStaff();
    }

    public void requireConsultationManageAccess() {
        requireAnyRole(UserRole.SALES_STAFF, UserRole.UNDERWRITING_STAFF, UserRole.ADMIN);
    }

    public void requireProposalManageAccess() {
        requireAnyRole(UserRole.SALES_STAFF, UserRole.UNDERWRITING_STAFF, UserRole.ADMIN);
    }

    public void requireInterviewManageAccess() {
        requireAnyRole(UserRole.SALES_STAFF, UserRole.UNDERWRITING_STAFF, UserRole.ADMIN);
    }

    public void requireUnderwritingOperationAccess() {
        requireUnderwritingStaff();
    }

    public void requireSalesOperationAccess() {
        requireSalesStaff();
    }

    public void requireEducationOperationAccess() {
        requireEducationStaff();
    }

    public void requireContractOperationAccess() {
        requireContractStaff();
    }

    public void requireBonusRequestManageAccess() {
        requireAdmin();
    }

    public void requireInquiryAnswerAccess() {
        requireStaffOrAdmin();
    }

    public void requireCustomerAccess(Customer customer) {
        if (customer == null || !isCustomer()) {
            return;
        }
        requireCustomerNoAccess(customer.getCustomerId());
    }

    public void requireCustomerIdAccess(Long customerId) {
        if (!canAccessCustomerId(customerId)) {
            throw ApiException.forbidden("본인 고객 데이터만 접근할 수 있습니다.");
        }
    }

    public boolean canAccessCustomerId(Long customerId) {
        AuthenticatedUser user = currentUser();
        if (user.role() != UserRole.CUSTOMER) {
            return true;
        }
        return customerId != null
                && user.linkedCustomerId() != null
                && user.linkedCustomerId().equals(customerId);
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
