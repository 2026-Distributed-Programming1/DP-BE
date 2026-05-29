package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.actor.Customer;

/**
 * 보험가입신청서 (InsuranceApplication)
 * UC: 보험 가입을 신청한다
 */
public class InsuranceApplication {

    private static int sequence = 0;

    private int applicationNumber;
    private Customer customer;
    private InsuranceProduct product;
    private LocalDateTime appliedAt;
    private String paymentMethod;
    private List<String> selectedSpecialTerms;
    private String status;

    public InsuranceApplication(int applicationNumber, Customer customer, InsuranceProduct product,
                                LocalDateTime appliedAt, String paymentMethod, List<String> selectedSpecialTerms) {
        this.applicationNumber = applicationNumber;
        this.customer = customer;
        this.product = product;
        this.appliedAt = appliedAt;
        this.paymentMethod = paymentMethod;
        this.selectedSpecialTerms = selectedSpecialTerms != null ? selectedSpecialTerms : new ArrayList<>();
    }

    public InsuranceApplication() {
        sequence += 1;
        this.applicationNumber = sequence;
        this.selectedSpecialTerms = new ArrayList<>();
    }

    private InsuranceApplication(boolean fromDb) {
        this.selectedSpecialTerms = new ArrayList<>();
    }

    public static InsuranceApplication fromDb(int applicationNumber, String customerId,
                                               String customerName, String productName,
                                               long monthlyPremium, String paymentMethod) {
        InsuranceApplication ia = new InsuranceApplication(true);
        ia.applicationNumber = applicationNumber;
        if (customerId != null) {
            ia.customer = new Customer(
                    customerId, customerName != null ? customerName : "", null, null, null);
        }
        if (productName != null) {
            ia.product = new InsuranceProduct(productName, null, monthlyPremium, null, null);
        }
        ia.paymentMethod = paymentMethod;
        return ia;
    }

    public void setCustomer(Customer customer) { this.customer = customer; }
    public Customer getCustomer() { return customer; }

    public void setProduct(InsuranceProduct product) { this.product = product; }
    public InsuranceProduct getProduct() { return product; }

    public void enterPersonalInfo(String name, String birthDate, String contact, String address) {
        System.out.println("  [시스템] 개인정보가 입력되었습니다.");
    }

    public void selectSpecialTerms(List<String> selectedSpecialTerms) {
        this.selectedSpecialTerms = selectedSpecialTerms;
    }

    public void selectPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean authenticate() {
        System.out.println("  [시스템] 본인인증이 완료되었습니다.");
        return true;
    }

    public void apply() {
        this.appliedAt = LocalDateTime.now();
    }

    public int getApplicationNumber() { return applicationNumber; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public List<String> getSelectedSpecialTerms() { return selectedSpecialTerms; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}