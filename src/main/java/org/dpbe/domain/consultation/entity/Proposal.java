package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;

/**
 * 제안서 (Proposal)
 * UC: 보험상품을 제안한다
 */
public class Proposal {

    private Long id;
    private String proposalNo;
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

    public Proposal() {}

    public void selectProduct(InsuranceProduct product) {
        this.insuranceProduct = product;
    }

    public void send() {
        this.sentAt = LocalDateTime.now();
        // 처리 필요
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProposalNo() { return proposalNo; }
    public void setProposalNo(String proposalNo) { this.proposalNo = proposalNo; }
    public int getProposalId() { return proposalId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public InsuranceProduct getInsuranceProduct() { return insuranceProduct; }
}