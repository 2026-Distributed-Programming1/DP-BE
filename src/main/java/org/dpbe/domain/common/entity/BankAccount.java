package org.dpbe.domain.common.entity;

/**
 * 계좌 정보 (BankAccount)
 *
 * 보험금 지급, 환급금 지급, 보험료 납입 등에 사용되는 계좌 정보를 담는 공통 클래스이다.
 * verify() 메서드를 통해 본인 명의 계좌 여부를 검증한다.
 */
public class BankAccount {

    private String bankName;          // 은행명
    private String accountNo;         // 계좌번호
    private String accountHolder;     // 예금주명
    private boolean verified;         // 인증 결과

    /** 생성자 - 빈 상태로 생성 */
    public BankAccount() {
        this.verified = false;
    }

    /** 입력 */
    public void enter(String bank, String no, String holder) {
        this.bankName = bank;
        this.accountNo = no;
        this.accountHolder = holder;
    }

    /**
     * 본인 명의 계좌 검증 (E1)
     * 외부 시스템(은행 API) 연동이 필요한 부분이므로 더미로 처리한다.
     */
    public boolean verify() {
        // 더미 로직: 모든 필드가 채워졌으면 인증 성공으로 간주
        if (bankName != null && accountNo != null && accountHolder != null) {
            this.verified = true;
            return true;
        }
        return false;
    }

    // Getter
    public String getBankName() { return bankName; }
    public String getAccountNo() { return accountNo; }
    public String getAccountHolder() { return accountHolder; }
    public boolean isVerified() { return verified; }
}
