package org.dpbe.domain.actor;

/**
 * 사용자 (User) - 추상 클래스
 *
 * Customer와 Employee의 공통 부모 클래스로, 업무 도메인 사용자의 공통 속성을 정의한다.
 * 로그인 계정과 세션 상태는 global.auth 패키지에서 별도로 관리한다.
 */
public abstract class User {

    protected String userId;       // 사용자 ID
    protected String name;         // 이름
    protected String contact;      // 연락처
    protected String email;        // 이메일

    protected User(String userId, String name, String contact, String email) {
        this.userId = userId;
        this.name = name;
        this.contact = contact;
        this.email = email;
    }

    public void updateProfile(String contact, String email) {
        this.contact = contact;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getEmail() { return email; }
}
