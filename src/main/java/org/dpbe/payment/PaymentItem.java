package org.dpbe.payment;


import org.dpbe.contract.Contract;

/**
 * 납입 항목 (PaymentItem) - N:M 매핑 클래스
 *
 * Payment와 Contract 사이의 N:M 관계를 풀어주는 매핑 클래스이다.
 * 어떤 납입 신청에서 어떤 계약에 얼마를 며칠치 납입하는지를 표현한다.
 * 회당 보험료(계약에서 자동 로드), 납입 횟수, 소계 금액으로 구성된다.
 */
public class PaymentItem {

    private Payment payment;                  // 납입 건
    private Contract contract;                // 대상 계약
    private long premiumPerCount;             // 회당 보험료 - contract에서 자동 로드
    private int count;                        // 납입 횟수
    private long subtotal;                    // 소계 금액

    /** 생성자 - 회당 보험료 자동 로드 */
    public PaymentItem(Payment payment, Contract contract) {
        this.payment = payment;
        this.contract = contract;
        if (contract != null) {
            this.premiumPerCount = contract.getMonthlyPremium();
        }
    }

    /** 납입 횟수 설정 */
    public void setCount(int count) {
        this.count = count;
        calculateSubtotal();
    }

    /** 소계 산출 = premiumPerCount × count */
    public long calculateSubtotal() {
        this.subtotal = this.premiumPerCount * this.count;
        return this.subtotal;
    }

    // Getter
    public Payment getPayment() { return payment; }
    public Contract getContract() { return contract; }
    public long getPremiumPerCount() { return premiumPerCount; }
    public int getCount() { return count; }
    public long getSubtotal() { return subtotal; }
}
