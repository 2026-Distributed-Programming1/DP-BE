package org.dpbe.payment;

import java.time.LocalDateTime;

/**
 * 미납 알림 자동 발송 설정 (OverdueNoticeSetting)
 *
 * 미납 고객에게 보내는 자동 알림의 발송 조건을 설정하는 클래스이다.
 * 시스템 단위의 설정값이므로 일반적으로 인스턴스가 1개만 존재한다.
 */
public class OverdueNoticeSetting {

    private boolean enabled;             // 자동 발송 활성화 여부 - 기본 false
    private int daysAfterDue;            // 발송 기준일 - 납입일 경과 일수
    private String messageTemplate;      // 발송 메시지 템플릿
    private LocalDateTime savedAt;       // 저장일시

    /** 생성자 - 기본값 enabled=false */
    public OverdueNoticeSetting() {
        this.enabled = false;
    }

    /** 자동 발송 활성화 토글 */
    public void toggleEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 발송 기준일 입력 */
    public void setDaysAfterDue(int days) {
        this.daysAfterDue = days;
    }

    /** 메시지 템플릿 설정 (저장 전 단계) */
    public void setMessageTemplate(String template) {
        this.messageTemplate = template;
    }

    /** 메시지 템플릿 미리보기 */
    public String previewMessage() {
        return this.messageTemplate;
    }

    /** 설정 저장 */
    public void save() {
        this.savedAt = LocalDateTime.now();
        System.out.println("[OverdueNoticeSetting] 설정 저장 완료 (활성: " + enabled
                + ", D+" + daysAfterDue + ")");
    }

    // Getter
    public boolean isEnabled() { return enabled; }
    public int getDaysAfterDue() { return daysAfterDue; }
    public String getMessageTemplate() { return messageTemplate; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
