package org.dpbe.domain.actor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;

/**
 * 고객 (Customer)
 */
public class Customer extends User {

    private Long id;
    private String customerId;
    private String phone;
    private String residentNo;
    private String address;
    private LocalDate birthDate;
    private List<BankAccount> registeredAccounts;
    private LocalDateTime registeredAt;

    /** DB 로딩용 생성자 */
    public Customer(String customerId, String name, String residentNo, String phone, String email) {
        super(customerId, name, phone, email);
        this.customerId = customerId;
        this.residentNo = residentNo;
        this.registeredAccounts = new ArrayList<>();
    }

    public void enterAddress(String address) { this.address = address; }
    public void enterBirthDate(LocalDate date) { this.birthDate = date; }
    public void registerAccount(BankAccount account) { this.registeredAccounts.add(account); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerNo() { return customerId; }
    public String getResidentNo() { return residentNo; }
    public String getAddress() { return address; }
    public LocalDate getBirthDate() { return birthDate; }
    public List<BankAccount> getRegisteredAccounts() { return registeredAccounts; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}