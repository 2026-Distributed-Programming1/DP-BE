package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;

/**
 * 제안서 (Proposal)
 * UC: 보험상품을 제안한다
 */
public class Proposal {

    private static int sequence = 0;

    private int proposalId;
    private LocalDateTime sentAt;
    private String customerName;
    private InsuranceProduct insuranceProduct;

    public Proposal(int proposalId, LocalDateTime sentAt, String customerName, InsuranceProduct insuranceProduct) {
        this.proposalId = proposalId;
        this.sentAt = sentAt;
        this.customerName = customerName;
        this.insuranceProduct = insuranceProduct;
    }

    public Proposal() {
        sequence += 1;
        this.proposalId = sequence;
    }

    public void selectProduct(InsuranceProduct product) {
        this.insuranceProduct = product;
    }

    public void send() {
        this.sentAt = LocalDateTime.now();
        System.out.println("  [시스템] 보험상품 제안서가 고객에게 발송되었습니다.");
    }

    public int getProposalId() { return proposalId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public InsuranceProduct getInsuranceProduct() { return insuranceProduct; }
}