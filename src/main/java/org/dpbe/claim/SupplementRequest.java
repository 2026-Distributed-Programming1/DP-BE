package org.dpbe.claim;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 보완 서류 요청 (SupplementRequest)
 *
 * 손해 조사 진행 중 고객에게 추가 서류를 요청해야 할 때 생성되는 클래스이다.
 * DamageInvestigation의 합성 부품으로, send() 메서드를 통해 고객에게
 * 알림톡 또는 문자로 보완 요청을 발송한다.
 */
public class SupplementRequest {

    private List<String> requestedItems;    // 요청 서류 항목
    private String message;                 // 메시지
    private LocalDateTime sentAt;           // 발송일시

    /** 생성자 */
    public SupplementRequest(List<String> items, String message) {
        this.requestedItems = new ArrayList<>(items);
        this.message = message;
    }

    /**
     * 고객에게 알림톡/문자 발송
     * 외부 시스템 연동이 필요하므로 더미로 처리한다.
     */
    public void send() {
        this.sentAt = LocalDateTime.now();
        System.out.println("[SupplementRequest] 보완 서류 요청 발송: " + requestedItems);
    }

    // Getter
    public List<String> getRequestedItems() { return requestedItems; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
}
