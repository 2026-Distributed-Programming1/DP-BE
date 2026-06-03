package org.dpbe.domain.consultation.entity;

import org.dpbe.domain.actor.Customer;
import java.time.LocalDateTime;

/**
 * 부활신청 (Revival)
 * UC: 계약 부활을 신청한다
 */
public class Revival {

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

    public Revival() {}

    public boolean checkEligibility() {
      // 처리 필요
        return true;
    }

    public long calculateUnpaidAmount() {
      // 처리 필요
        return unpaidAmount;
    }

    public boolean pay(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        // 처리 필요
        return true;
    }

    public boolean authenticate() {
      // 처리 필요
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
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public long getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(long unpaidAmount) { this.unpaidAmount = unpaidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}