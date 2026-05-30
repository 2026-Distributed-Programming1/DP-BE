package org.dpbe.domain.contract.entity;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.ContractStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 보험계약 (Contract)
 *
 * InsuranceContract(7·8 도메인용)과 Contract(6 도메인용)를 통합한 클래스.
 * - 파라미터 생성자: 7·8 도메인에서 계약번호·증권번호 자동 부여
 * - 기본 생성자: 6 도메인 계약 관리 담당자 화면에서 수동 입력
 */
public class Contract {

    private static int sequence = 0;
    private static int policySequence = 0;

    // 7·8 도메인 공통 필드
    private Long id;                    // DB 대리키(PK)
    private String contractNo;
    private String policyNo;
    private Customer customer;
    private LocalDate contractDate;     // alias: startDate
    private LocalDate expiryDate;       // alias: endDate
    private long monthlyPremium;

    // 6 도메인 추가 필드
    private String insuranceType;
    private ContractStatus status;
    private Boolean isExpiringSoon;
    private Integer totalPayCount;
    private Integer paidCount;
    private LocalDate lastPaymentDate;
    private Boolean isOverdue;
    private Integer overdueCount;
    private List<String> specialClauses;
    private List<Long> clausePremiums;

    /** 7·8 도메인 생성자 — 계약번호·증권번호 자동 부여 */
    public Contract(Customer customer, LocalDate contractDate, LocalDate expiryDate, long monthlyPremium) {
        sequence += 1;
        policySequence += 1;
        this.contractNo = "CON" + String.format("%05d", sequence);
        this.policyNo   = "POL" + String.format("%05d", policySequence);
        this.customer      = customer;
        this.contractDate  = contractDate;
        this.expiryDate    = expiryDate;
        this.monthlyPremium = monthlyPremium;
        this.status        = ContractStatus.NORMAL;
        this.isExpiringSoon = false;
        this.isOverdue     = false;
        this.specialClauses = new ArrayList<>();
        this.clausePremiums = new ArrayList<>();
    }

    /** 6 도메인 기본 생성자 */
    public Contract() {
        this.specialClauses = new ArrayList<>();
        this.clausePremiums = new ArrayList<>();
        this.status        = ContractStatus.NORMAL;
        this.isExpiringSoon = false;
        this.isOverdue     = false;
    }

    /** DB 로딩용 생성자 */
    public Contract(String contractNo, String policyNo, Customer customer,
                    LocalDate contractDate, LocalDate expiryDate, long monthlyPremium,
                    String insuranceType, ContractStatus status,
                    Boolean isExpiringSoon, Boolean isOverdue, Integer overdueCount) {
        this.contractNo     = contractNo;
        this.policyNo       = policyNo;
        this.customer       = customer;
        this.contractDate   = contractDate;
        this.expiryDate     = expiryDate;
        this.monthlyPremium = monthlyPremium;
        this.insuranceType  = insuranceType;
        this.status         = status != null ? status : ContractStatus.NORMAL;
        this.isExpiringSoon = isExpiringSoon != null ? isExpiringSoon : false;
        this.isOverdue      = isOverdue != null ? isOverdue : false;
        this.overdueCount   = overdueCount;
        this.specialClauses = new ArrayList<>();
        this.clausePremiums = new ArrayList<>();
    }

    /** DB 셸 객체용 — 참조 복원에 필요한 필드만 지정 */
    public static Contract shellOf(String contractNo, Customer customer, long monthlyPremium) {
        Contract c = new Contract();
        c.contractNo    = contractNo;
        c.customer      = customer;
        c.monthlyPremium = monthlyPremium;
        return c;
    }

    // ── 클래스 다이어그램 정의 메서드 (스텁) ──────────────────────────────────

    public void search() {}
    public void getDetail() {}
    public void edit() {}
    public void navigateToExpiry() {}
    public void navigateToStats() {}
    public void retry() {}
    public void showNoSpecialClause() {}
    public void showNotFoundMessage() {}

    /** 만기 임박 여부 (30일 이내) */
    public boolean isMaturityNear() {
        if (expiryDate == null) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        return days >= 0 && days <= 30;
    }

    /** 계약 상태 갱신 */
    public void updateStatus(String newStatus) {
        if ("정상".equals(newStatus) || "정상유지".equals(newStatus)) {
            this.status = ContractStatus.NORMAL;
        } else if ("만기".equals(newStatus)) {
            this.status = ContractStatus.EXPIRED;
        } else if ("해지".equals(newStatus)) {
            this.status = ContractStatus.CANCELLED;
        } else if ("실효".equals(newStatus)) {
            this.status = ContractStatus.LAPSED;
        }
    }

    public void changePaymentMethod(String method, BankAccount account) {}

    public boolean verifyAccount(int amount, String code) { return true; }

    public void getCertificatePDF() {}

    public void getPolicyPDF() {}

    public void initiateCancellation() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long   getId() { return id; }
    public void   setId(Long id) { this.id = id; }
    public String getContractNo() { return contractNo; }
    public void   setContractNo(String contractNo) { this.contractNo = contractNo; }
    public void   setPolicyNo(String policyNo) { this.policyNo = policyNo; }

    public String getPolicyNo() { return policyNo; }

    public Customer getCustomer() { return customer; }

    public LocalDate getContractDate() { return contractDate; }
    public LocalDate getExpiryDate()   { return expiryDate; }
    public long      getMonthlyPremium() { return monthlyPremium; }
    public void      setMonthlyPremium(long monthlyPremium) { this.monthlyPremium = monthlyPremium; }

    /** Alias: startDate == contractDate */
    public LocalDate getStartDate() { return contractDate; }
    public void      setStartDate(LocalDate startDate) { this.contractDate = startDate; }

    /** Alias: endDate == expiryDate */
    public LocalDate getEndDate() { return expiryDate; }
    public void      setEndDate(LocalDate endDate) { this.expiryDate = endDate; }

    public String getInsuranceType() { return insuranceType; }
    public void   setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }

    public ContractStatus getStatus() { return status; }
    public void           setStatus(ContractStatus status) { this.status = status; }

    public Boolean getIsExpiringSoon() { return isExpiringSoon; }
    public void    setIsExpiringSoon(Boolean isExpiringSoon) { this.isExpiringSoon = isExpiringSoon; }

    public Integer getTotalPayCount() { return totalPayCount; }
    public void    setTotalPayCount(Integer totalPayCount) { this.totalPayCount = totalPayCount; }

    public Integer getPaidCount() { return paidCount; }
    public void    setPaidCount(Integer paidCount) { this.paidCount = paidCount; }

    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

    public Boolean getIsOverdue() { return isOverdue; }
    public void    setIsOverdue(Boolean isOverdue) { this.isOverdue = isOverdue; }

    public Integer getOverdueCount() { return overdueCount; }
    public void    setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }

    public List<String> getSpecialClauses() { return specialClauses; }
}