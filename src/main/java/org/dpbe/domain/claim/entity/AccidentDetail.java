package org.dpbe.domain.claim.entity;

import java.time.LocalDate;
import org.dpbe.domain.common.enums.AccidentSubType;

/**
 * 사고 상세 정보 - 재해 청구 시 (AccidentDetail)
 *
 * 재해로 인한 보험금 청구 시 사고에 대한 상세 정보를 담는 클래스이다.
 * ClaimRequest의 합성 부품으로, 청구 유형이 "재해"일 때만 생성된다.
 */
public class AccidentDetail {

    private AccidentSubType accidentSubType;    // 사고 유형 - 일반재해/교통재해
    private String content;                     // 사고 내용
    private LocalDate date;                     // 사고 날짜
    private String location;                    // 사고 장소

    /** 생성자 - 빈 상태로 생성 */
    public AccidentDetail() {
    }

    /** 상세 정보 입력 */
    public void enter(AccidentSubType subType, String content, LocalDate date, String location) {
        this.accidentSubType = subType;
        this.content = content;
        this.date = date;
        this.location = location;
    }

    // Getter
    public AccidentSubType getAccidentSubType() { return accidentSubType; }
    public String getContent() { return content; }
    public LocalDate getDate() { return date; }
    public String getLocation() { return location; }
}
