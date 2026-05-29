package org.dpbe.domain.actor;

import org.dpbe.domain.common.enums.ChannelType;

/**
 * 지원자 (Applicant)
 * 판매채널 채용 심사 대상 지원자를 나타내는 클래스이다.
 */
public class Applicant {
    private String applicantId;      // 지원자 ID
    private String name;             // 이름
    private ChannelType channelType; // 지원 채널 유형 (enum)

    public Applicant(String applicantId, String name, ChannelType channelType) {
        this.applicantId = applicantId;
        this.name = name;
        this.channelType = channelType;
    }

    public void getDetail() {}

    // Getters
    public String getApplicantId() { return applicantId; }
    public String getName() { return name; }
    public ChannelType getChannelType() { return channelType; }
}