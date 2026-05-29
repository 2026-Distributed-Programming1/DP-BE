package org.dpbe.actor;

/**
 * 사용자 (User) - 추상 클래스
 *
 * Customer와 Employee의 공통 부모 클래스로, 시스템 사용자의 공통 속성과 행위를 정의한다.
 */
public abstract class User {

    private static int sequence = 0;    // userId 자동 부여용

    protected String userId;       // 사용자 ID
    protected String name;         // 이름
    protected String contact;      // 연락처
    protected String email;        // 이메일
    protected String password;     // 비밀번호
    protected boolean loggedIn;    // 로그인 상태

    /** 생성자 - userId 자동 부여 */
    public User(String name, String contact, String email) {
        sequence += 1;
        this.userId = "USR" + String.format("%05d", sequence);
        this.name = name;
        this.contact = contact;
        this.email = email;
        this.loggedIn = false;
    }

    /** DB 로딩용 생성자 - 자동증가 없이 userId 직접 지정 */
    protected User(String userId, String name, String contact, String email) {
        this.userId = userId;
        this.name = name;
        this.contact = contact;
        this.email = email;
        this.loggedIn = false;
    }

    /** 로그인 */
    public boolean login(String id, String pw) {
        if (this.userId.equals(id) && this.password != null && this.password.equals(pw)) {
            this.loggedIn = true;
            return true;
        }
        return false;
    }

    /** 로그아웃 */
    public void logout() {
        this.loggedIn = false;
    }

    /** 정보 수정 */
    public void updateProfile(String contact, String email) {
        this.contact = contact;
        this.email = email;
    }

    // Getter
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getEmail() { return email; }
    public boolean isLoggedIn() { return loggedIn; }

    public void setPassword(String password) { this.password = password; }
}
