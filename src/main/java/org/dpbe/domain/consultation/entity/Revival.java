package org.dpbe.domain.consultation.entity;

import org.dpbe.domain.actor.Customer;
import java.time.LocalDateTime;

/**
 * 부활신청 (Revival)
 * UC: 계약 부활을 신청한다
 */
public class Revival {

    private static int sequence = 0;

    private Long id;
    private String revivalNo;
    private int revivalNumber;
    private Customer customer;
    private String contractNo;
    private LocalDateTime appliedAt;
    private long unpaidAmount;
    private String paymentMethod;
    private String contact;

    public Revival(int revivalNumber, LocalDateTime appliedAt, long unpaidAmount, String paymentMethod) {
        this.revivalNumber = revivalNumber;
        this.appliedAt = appliedAt;
        this.unpaidAmount = unpaidAmount;
        this.paymentMethod = paymentMethod;
    }

    public Revival() {
        sequence += 1;
        this.revivalNumber = sequence;
    }

    public boolean checkEligibility() {
        System.out.println("  [시스템] 부활 가능 여부를 확인합니다.");
        return true;
    }

    public long calculateUnpaidAmount() {
        System.out.println("  [시스템] 미납보험료 및 이자를 산출합니다.");
        return unpaidAmount;
    }

    public boolean pay(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        System.out.println("  [시스템] 미납보험료가 납입되었습니다.");
        return true;
    }

    public boolean authenticate() {
        System.out.println("  [시스템] 본인인증이 완료되었습니다.");
        return true;
    }

    public void submit() {
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRevivalNo() { return revivalNo; }
    public void setRevivalNo(String revivalNo) { this.revivalNo = revivalNo; }
    public int getRevivalNumber() { return revivalNumber; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public long getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(long unpaidAmount) { this.unpaidAmount = unpaidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}