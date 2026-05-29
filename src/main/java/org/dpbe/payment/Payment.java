package org.dpbe.payment;

import org.dpbe.actor.Customer;
import org.dpbe.common.BankAccount;
import org.dpbe.contract.Contract;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.enums.PaymentMethod;
import org.dpbe.enums.PaymentStatus;

/**
 * 보험료 납입 (Payment)
 *
 * 고객이 가입한 보험계약에 대해 보험료를 납입하기 위해 생성되는 클래스이다.
 * 「보험료를 납입한다」 유스케이스의 중심 클래스로, 한 번의 납입 신청에서
 * 여러 계약을 동시에 처리할 수 있다(N:M 관계는 PaymentItem으로 분리).
 */
public class Payment {

    private static int sequence = 0;          // 납입 신청번호 자동 부여용

    private String paymentNo;                 // 납입 신청번호
    private Customer customer;                // 고객
    private List<PaymentItem> items;          // 납입 항목 목록 - N:M 매핑
    private PaymentMethod paymentMethod;      // 납입 방법
    private BankAccount account;              // 납입 계좌
    private long totalAmount;                 // 총 신청 금액
    private long discountedAmount;            // 할인 적용 금액
    private long earlyDiscount;               // 선납 할인액
    private LocalDateTime requestedAt;        // 신청일시
    private PaymentStatus status;             // 상태

    /** 생성자 - 납입 신청번호 자동 부여 */
    public Payment(Customer customer) {
        sequence += 1;
        this.paymentNo = "PAY" + String.format("%05d", sequence);
        this.customer = customer;
        this.items = new ArrayList<>();
        this.status = PaymentStatus.DRAFT;
    }

    /** 납입 대상 계약 1건 이상 선택 - PaymentItem 생성 */
    public void selectContracts(List<Contract> contracts) {
        for (Contract contract : contracts) {
            this.items.add(new PaymentItem(this, contract));
        }
    }

    /** 계약별 납입 횟수 입력 */
    public void enterPaymentCount(PaymentItem item, int count) {
        if (this.items.contains(item)) {
            item.setCount(count);
        }
    }

    /**
     * 잔여 납입 횟수 이하 검증
     * 본 구현에서는 납입 횟수가 양수인지만 확인한다.
     * (실제 잔여 회차 정보는 PaymentStatus 클래스에 있으나, 6 도메인 미구현 상태이므로 단순 검증)
     */
    public boolean validatePaymentCount() {
        for (PaymentItem item : items) {
            if (item.getCount() <= 0) {
                return false;
            }
        }
        return !items.isEmpty();
    }

    /** 납입 방법 선택 (A2) */
    public void selectPaymentMethod(PaymentMethod method) {
        this.paymentMethod = method;
    }

    /** 등록된 계좌 선택 */
    public void selectExistingAccount(BankAccount account) {
        this.account = account;
    }

    /** 새 계좌 입력 (A3) */
    public void registerNewAccount(String bank, String no, String holder) {
        BankAccount newAccount = new BankAccount();
        newAccount.enter(bank, no, holder);
        this.account = newAccount;
    }

    /** 계좌 인증 */
    public boolean verifyAccount() {
        if (this.account != null) {
            return this.account.verify();
        }
        return false;
    }

    /** 총 신청 금액 + 선납 할인 산출 */
    public long calculateTotal() {
        long sum = 0;
        for (PaymentItem item : items) {
            sum += item.calculateSubtotal();
        }
        this.totalAmount = sum;
        // 선납 할인: 2회분 이상 한꺼번에 납입 시 1% 할인 (본 구현 단순화)
        long maxCount = 0;
        for (PaymentItem item : items) {
            if (item.getCount() > maxCount) maxCount = item.getCount();
        }
        if (maxCount >= 2) {
            this.earlyDiscount = (long) (sum * 0.01);
        } else {
            this.earlyDiscount = 0;
        }
        this.discountedAmount = this.totalAmount - this.earlyDiscount;
        return this.discountedAmount;
    }

    /** 신청 - requestedAt=now() */
    public void submit() {
        if (!validatePaymentCount() || account == null || !account.isVerified()) {
            handleProcessingError();
            return;
        }
        calculateTotal();
        this.requestedAt = LocalDateTime.now();
        this.status = PaymentStatus.COMPLETED;
        System.out.println("[Payment] 보험료 납입 신청 완료: " + paymentNo + ", 금액: " + discountedAmount);
    }

    /** 납입 처리 오류 (E1) */
    public void handleProcessingError() {
        this.status = PaymentStatus.ERROR;
        System.out.println("[Payment] 납입 처리 오류 발생: " + paymentNo);
    }

    /** 취소 */
    public void cancel() {
        System.out.println("[Payment] 납입 신청 취소: " + paymentNo);
    }

    // Getter
    public String getPaymentNo() { return paymentNo; }
    public Customer getCustomer() { return customer; }
    public List<PaymentItem> getItems() { return items; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BankAccount getAccount() { return account; }
    public long getTotalAmount() { return totalAmount; }
    public long getDiscountedAmount() { return discountedAmount; }
    public long getEarlyDiscount() { return earlyDiscount; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    // DB 복원용 setter
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public void setDiscountedAmount(long discountedAmount) { this.discountedAmount = discountedAmount; }
}
