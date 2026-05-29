package org.dpbe.domain.actor;

import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.domain.consultation.entity.PolicyApplication;
import org.dpbe.domain.common.enums.ChannelType;

/**
 * 판매채널 (SalesChannel)
 * Designer, Agency 의 부모 클래스 (Generalization)
 */
public class SalesChannel {

    private String channelId;        // 채널 ID
    private String channelName;      // 채널명
    private ChannelType channelType; // 채널 유형 - 설계사/대리점 (enum)

    public SalesChannel(String channelId, String channelName, String location) {
        this.channelId = channelId;
        this.channelName = channelName;
    }

    // ===== 다이어그램 메서드 =====
    public void getActivityDetail() {}

    // ===== Runner 호출 중이므로 유지 =====
    public void acceptConsultation(ConsultationRequest request) {
        request.accept();
        System.out.println("  [" + channelName + "] 상담 요청을 수락했습니다.");
    }

    public PolicyApplication createPolicyApplication() {
        return new PolicyApplication();
    }

    public String getName() { return channelName; }
    public String getChannelId() { return channelId; }
}