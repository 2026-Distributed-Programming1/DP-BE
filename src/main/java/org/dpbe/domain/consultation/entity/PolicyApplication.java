package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.actor.Customer;

/**
 * 청약서 (PolicyApplication)
 * UC: 청약서를 작성한다
 */
public class PolicyApplication {

    private static int sequence = 0;

    private Long id;
    private String applicationNo;
    private int applicationNumber;
    private LocalDateTime submittedAt;
    private LocalDateTime uploadedAt;
    private Customer customer;
    private String customerName;
    private String productName;
    private int period;
    private String paymentMethod;
    private String status;

    public PolicyApplication(int applicationNumber, LocalDateTime submittedAt, Customer customer,
                             String customerName, String productName, int period, String paymentMethod) {
        this.applicationNumber = applicationNumber;
        this.submittedAt = submittedAt;
        this.customer = customer;
        this.customerName = customerName;
        this.productName = productName;
        this.period = period;
        this.paymentMethod = paymentMethod;
    }

    public PolicyApplication() {
        sequence += 1;
        this.applicationNumber = sequence;
    }

    private PolicyApplication(boolean fromDb) {}

    public static PolicyApplication fromDb(int applicationNumber, String customerId,
                                            String customerName, String productName,
                                            int period, String paymentMethod) {
        PolicyApplication pa = new PolicyApplication(true);
        pa.applicationNumber = applicationNumber;
        if (customerId != null) {
            pa.customer = new Customer(
                    customerId, customerName != null ? customerName : "", null, null, null);
        }
        pa.customerName = customerName;
        pa.productName = productName;
        pa.period = period;
        pa.paymentMethod = paymentMethod;
        return pa;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.customerName = customer.getName();
    }

    public Customer getCustomer() { return customer; }

    public void enterCustomerInfo(String customerName, String birthDate, String contact, String address) {
        this.customerName = customerName;
    }

    public void selectProduct(String productName, int period, String paymentMethod) {
        this.productName = productName;
        this.period = period;
        this.paymentMethod = paymentMethod;
    }

    public void attachSignature(String file) {
        this.uploadedAt = LocalDateTime.now();
        System.out.println("  [시스템] 서명 파일이 첨부되었습니다: " + file);
    }

    public void requestElectronicSignature() {
        System.out.println("  [시스템] 고객에게 전자서명 요청이 발송되었습니다.");
    }

    public void submit() {
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public int getApplicationNumber() { return applicationNumber; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getCustomerName() { return customerName; }
    public String getProductName() { return productName; }
    public int getPeriod() { return period; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}