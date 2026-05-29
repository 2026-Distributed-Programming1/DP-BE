package org.dpbe.domain.inquiry.entity;

/**
 * 고객센터 페이지 (CustomerCenterPage)
 * 문의한다 유스케이스의 주요 도메인 클래스이다.
 */
public class CustomerCenterPage {
    private String activeTab; // 활성 탭

    public void switchTab() {
        // 탭 전환: 활성 탭을 순환하며 전환한다
        if (activeTab == null || activeTab.equals("FAQ")) {
            activeTab = "1:1문의";
        } else if (activeTab.equals("1:1문의")) {
            activeTab = "문의내역";
        } else {
            activeTab = "FAQ";
        }
    }
}