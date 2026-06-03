package org.dpbe.domain.claim.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.entity.Attachment;
import org.dpbe.domain.common.enums.DispatchRecordStatus;
import org.dpbe.global.exception.ApiException;

/**
 * 현장 출동 기록 (DispatchRecord)
 *
 * 현장출동 직원이 사고 현장에서 작성하는 보고서 클래스이다.
 * 사고 현장의 사진(전경/파손/번호판/블랙박스 카테고리별), 경찰 출동 여부, 견인 필요 여부,
 * 현장 특이사항 및 요원 소견을 입력받는다.
 */
public class DispatchRecord {

    private Long id;                            // DB 대리키(PK)
    private String recordId;                    // 기록 ID
    private Dispatch dispatch;                  // 출동 건
    private List<Attachment> photos;            // 사진 목록 - 전경/파손/번호판/블랙박스
    private boolean policeRequired;             // 경찰 출동 여부
    private boolean towingRequired;             // 견인 필요 여부
    private String notes;                       // 현장 특이사항 및 요원 소견
    private LocalDateTime transmittedAt;        // 전송일시
    private DispatchRecordStatus status;        // 상태

    /** 신규 기록 생성자 */
    public DispatchRecord(Dispatch dispatch) {
        this.dispatch = dispatch;
        this.photos = new ArrayList<>();
        this.status = DispatchRecordStatus.DRAFT;
    }

    /**
     * 사진 카테고리별 업로드
     * 카테고리 정보는 Attachment의 의미상 속성이지만 다이어그램에 별도 필드는 없으므로
     * 본 구현에서는 단순히 사진 목록에 추가하는 방식으로 처리한다.
     */
    public void uploadPhoto(String category, Attachment photo) {
        this.photos.add(photo);
        // 처리 필요
    }

    /** 사진 삭제 */
    public void removePhoto(Attachment photo) {
        this.photos.remove(photo);
    }

    /** 경찰 출동 여부 선택 */
    public void setPoliceRequired(boolean required) {
        this.policeRequired = required;
    }

    /** 견인 필요 여부 선택 */
    public void setTowingRequired(boolean required) {
        this.towingRequired = required;
    }

    /** 특이사항 입력 */
    public void enterNotes(String notes) {
        this.notes = notes;
    }

    /** 필수 사진/항목 검증 (E1) */
    public boolean validateRequired() {
        return photos != null && !photos.isEmpty();
    }

    /** 기록 전송 - transmittedAt=now() */
    public void transmit() {
        if (!validateRequired()) {
            throw ApiException.badRequest("[E1] 현장 사진을 1장 이상 첨부해야 합니다.");
        }
        this.transmittedAt = LocalDateTime.now();
        this.status = DispatchRecordStatus.TRANSMITTED;
    }

    // Getter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public Dispatch getDispatch() { return dispatch; }
    public List<Attachment> getPhotos() { return photos; }
    public boolean isPoliceRequired() { return policeRequired; }
    public boolean isTowingRequired() { return towingRequired; }
    public String getNotes() { return notes; }
    public LocalDateTime getTransmittedAt() { return transmittedAt; }
    public void setTransmittedAt(LocalDateTime transmittedAt) { this.transmittedAt = transmittedAt; }
    public DispatchRecordStatus getStatus() { return status; }
    public void setStatus(DispatchRecordStatus status) { this.status = status; }
}
