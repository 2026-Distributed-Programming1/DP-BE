package org.dpbe.domain.consultation.entity;

/**
 * 보험상품 (InsuranceProduct)
 * 🌟 Entity 개념
 */
public class InsuranceProduct {

    private String productName;
    private String type;
    private long monthlyPremium;
    private String coverage;
    private String specialTerms;


    public InsuranceProduct(String productName, String type, long monthlyPremium,
                            String coverage, String specialTerms) {
        this.productName = productName;
        this.type = type;
        this.monthlyPremium = monthlyPremium;
        this.coverage = coverage;
        this.specialTerms = specialTerms;
    }


    public InsuranceProduct() {}

    public String getProductName() { return productName; }
    public String getType() { return type; }
    public long getMonthlyPremium() { return monthlyPremium; }
    public String getCoverage() { return coverage; }
    public String getSpecialTerms() { return specialTerms; }
}