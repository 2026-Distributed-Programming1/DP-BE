package org.dpbe.domain.actor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;

/**
 * 고객 (Customer)
 *
 * 시스템에 등록된 고객을 표현하는 클래스로, User를 상속한다.
 * 보험 가입, 보험금 청구, 사고 접수, 문의 등 다양한 유스케이스의 시작점이 된다.
 *
 * 클래스 다이어그램 기준 필드: customerId(고객번호), name(User 상속), phone
 * ※ 7·8 도메인 호환을 위해 추가 필드를 유지한다.
 */
public class Customer extends User {

    private static int sequence = 0;    // 고객번호 자동 부여용
    private Long id;                     // DB 대리키(PK)
    private String customerId;           // 고객번호 (클래스 다이어그램 기준)
    private String phone;                // 연락처
    private String residentNo;           // 주민등록번호
    private String address;              // 주소
    private LocalDate birthDate;         // 생년월일
    private List<BankAccount> registeredAccounts;    // 등록된 계좌 목록
    private LocalDateTime registeredAt;              // 가입일시

    /** 생성자 - 고객번호 자동 부여 */
    public Customer(String name, String residentNo, String contact, String email) {
        super(name, contact, email);
        sequence += 1;
        this.customerId = "CUS" + String.format("%05d", sequence);
        this.residentNo = residentNo;
        this.registeredAccounts = new ArrayList<>();
        this.registeredAt = LocalDateTime.now();
    }

    /** DB 로딩용 생성자 - 자동증가 없이 customerId 직접 지정 */
    public Customer(String customerId, String name, String residentNo, String phone, String email) {
        super(customerId, name, phone, email);
        this.customerId = customerId;
        this.residentNo = residentNo;
        this.registeredAccounts = new ArrayList<>();
    }

    /** 정보 수정 */
    public void updateInfo() {
        System.out.println("고객 정보(주소: " + address + ", 연락처: " + getContact() + ")가 업데이트되었습니다.");
    }

    /** 주소 입력 */
    public void enterAddress(String address) {
        this.address = address;
    }

    /** 생년월일 입력 */
    public void enterBirthDate(LocalDate date) {
        this.birthDate = date;
    }

    /** 계좌 등록 */
    public void registerAccount(BankAccount account) {
        this.registeredAccounts.add(account);
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    /** Alias: customerNo == customerId (7·8 도메인 호환) */
    public String getCustomerNo() { return customerId; }
    public String getResidentNo() { return residentNo; }
    public String getAddress() { return address; }
    public LocalDate getBirthDate() { return birthDate; }
    public List<BankAccount> getRegisteredAccounts() { return registeredAccounts; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}