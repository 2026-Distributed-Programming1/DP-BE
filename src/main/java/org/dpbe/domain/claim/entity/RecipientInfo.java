package org.dpbe.domain.claim.entity;

import org.dpbe.domain.actor.Customer;

/**
 * 수령인 정보 (RecipientInfo)
 *
 * 보험금을 실제로 수령할 사람의 정보를 담는 클래스이다.
 * ClaimRequest의 합성 부품으로, 본인인증 결과에 따라 자동으로 채워진다.
 */
public class RecipientInfo {

    private String name;          // 수령인 이름
    private String residentNo;    // 수령인 주민등록번호
    private String contact;       // 수령인 휴대전화번호

    /** DB 복원용 생성자 - 이름만 있는 경우 */
    public RecipientInfo(String name) {
        this.name = name;
    }

    /** 생성자 - 본인인증 결과로 자동 입력 */
    public RecipientInfo(Customer customer) {
        if (customer != null) {
            this.name = customer.getName();
            this.residentNo = customer.getResidentNo();
            this.contact = customer.getContact();
        }
    }

    /** 연락처 변경 */
    public void changeContact(String contact) {
        this.contact = contact;
    }

    // Getter
    public String getName() { return name; }
    public String getResidentNo() { return residentNo; }
    public String getContact() { return contact; }
}
