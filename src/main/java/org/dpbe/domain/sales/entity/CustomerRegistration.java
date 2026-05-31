package org.dpbe.domain.sales.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.enums.InsuranceType;

/**
 * 고객 정보 등록 (CustomerRegistration)
 * 판매채널이 확보한 고객 정보를 시스템에 최초 등록하는 클래스이다.
 */
public class CustomerRegistration {
    private Long id;
    private String customerId;              // 고객번호 - 자동부여
    private String name;                    // 이름 필수
    private String ssn;                     // 주민등록번호 필수
    private Boolean isSsnMasked = true;            // 뒷자리 마스킹 여부
    private String phone;                   // 연락처 필수
    private String address;                 // 주소 선택
    private String contractNo;              // 계약번호 - 자동부여
    private InsuranceType insuranceType;    // 보험종류 - 생명/건강/자동차/화재 필수 (enum)
    private LocalDate contractDate;         // 계약일 필수
    private LocalDate expiryDate;           // 만료일 필수
    private Long monthlyPremium;            // 월 보험료 필수
    private List<String> specialClauses = new ArrayList<>();    // 특약 정보 선택

//    public CustomerRegistration() {
//        this.isSsnMasked = true;
//        this.specialClauses = new ArrayList<>();
//    }

    public CustomerRegistration() {}

    /** DB 로딩용 생성자 */
    public CustomerRegistration(String customerId, String contractNo, String name, String ssn, String phone, InsuranceType insuranceType, LocalDate contractDate, LocalDate expiryDate, Long monthlyPremium) {
        this.customerId = customerId;
        this.contractNo = contractNo;
        this.name = name;
        this.ssn = ssn;
        this.phone = phone;
        this.insuranceType = insuranceType;
        this.contractDate = contractDate;
        this.expiryDate = expiryDate;
        this.monthlyPremium = monthlyPremium;
    }

    public void toggleSsnMask() {
        this.isSsnMasked = !this.isSsnMasked;
    }

    public void searchAddress() {}

    public void addSpecialClause() {}

    public void removeSpecialClause() {}

    public Boolean validateRequired() {
        return name != null && !name.isEmpty()
                && ssn != null && !ssn.isEmpty()
                && phone != null && !phone.isEmpty()
                && insuranceType != null
                && contractDate != null
                && expiryDate != null
                && monthlyPremium != null && monthlyPremium > 0;
    }

    public Boolean validateFormat() {
        if (ssn == null || !ssn.replaceAll("-", "").matches("\\d{13}")) return false;
        if (phone == null || !phone.replaceAll("-", "").matches("\\d{10,11}")) return false;
        return true;
    }

    public Boolean validateDuplicate() {
        return true;
    }

    public void highlightError() {}

    public void showDuplicateError() {}

    public void assignIds() {
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14);
        this.customerId = "CU-" + timestamp;
        this.contractNo = "CN-" + timestamp;
    }

    public void showSuccessPopup() {}

    public void save() {}

    public void edit() {}

    public void openCalendar() {}

    // Runner에서 실제 사용하는 getter/setter만 유지
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getContractNo() { return contractNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public Boolean getIsSsnMasked() { return isSsnMasked; }
    public String getMaskedSsn() {
        if (ssn == null || ssn.length() < 7) return ssn;
        return ssn.substring(0, 6) + "-" + ssn.charAt(6) + "******";
    }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public InsuranceType getInsuranceType() { return insuranceType; }
    public void setInsuranceType(InsuranceType insuranceType) { this.insuranceType = insuranceType; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Long getMonthlyPremium() { return monthlyPremium; }
    public void setMonthlyPremium(Long monthlyPremium) { this.monthlyPremium = monthlyPremium; }
    public List<String> getSpecialClauses() { return specialClauses; }
    public void addSpecialClause(String clause) { this.specialClauses.add(clause); }
}