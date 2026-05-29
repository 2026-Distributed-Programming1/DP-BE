package org.dpbe.contract;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.enums.CustomerResponse;

/**
 * 만기 계약 관리 (ExpiringContractManagement)
 * 만기 계약을 관리한다 유스케이스의 주요 도메인 클래스이다.
 */
public class ExpiringContractManagement {
    private String contractNo;            // 계약번호
    private String contractorName;        // 계약자명
    private String insuranceType;         // 보험종류
    private LocalDate expiryDate;         // 만료일
    private Integer remainingDays;        // 잔여 일수
    private String phone;                 // 전화번호
    private String email;                 // 이메일
    private Boolean isRenewable;          // 갱신 가능 여부
    private Long expectedPremium;         // 갱신 시 예상 보험료
    private LocalDateTime noticeDate;     // 안내 일시
    private String noticeMemo;            // 메모
    private CustomerResponse customerResponse; // 고객 응답 - 갱신/해지/추후결정 (enum)
    private Long renewalPremium;          // 갱신 후 월 보험료 A1
    private Long premiumDiff;             // 변동액 A1

    public void sendNoticeSms() {}

    public void saveNoticeRecord() {}

    public void notifyManager() {}

    public void confirmRenewal() {}

    public void saveRenewalContract() {}

    public void switchToTermination() {}

    public void sendPendingAlert() {}

    public void showExpiredBanner() {}

    public void disableRenewalTab() {}

    public void showSmsError() {}

    public void switchTab() {}

    public void closePopup() {}

    public void updateHistoryTab() {}

    // Getters / Setters
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }
    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getIsRenewable() { return isRenewable; }
    public void setIsRenewable(Boolean isRenewable) { this.isRenewable = isRenewable; }
    public Long getExpectedPremium() { return expectedPremium; }
    public void setExpectedPremium(Long expectedPremium) { this.expectedPremium = expectedPremium; }
    public LocalDateTime getNoticeDate() { return noticeDate; }
    public void setNoticeDate(LocalDateTime noticeDate) { this.noticeDate = noticeDate; }
    public String getNoticeMemo() { return noticeMemo; }
    public void setNoticeMemo(String noticeMemo) { this.noticeMemo = noticeMemo; }
    public CustomerResponse getCustomerResponse() { return customerResponse; }
    public void setCustomerResponse(CustomerResponse customerResponse) { this.customerResponse = customerResponse; }
    public Long getRenewalPremium() { return renewalPremium; }
    public void setRenewalPremium(Long renewalPremium) { this.renewalPremium = renewalPremium; }
    public Long getPremiumDiff() { return premiumDiff; }
    public void setPremiumDiff(Long premiumDiff) { this.premiumDiff = premiumDiff; }
}
