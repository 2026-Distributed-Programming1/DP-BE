package org.dpbe.domain.claim.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.common.entity.Attachment;

/**
 * 현장 출동 사진 메타데이터.
 *
 * 실제 파일은 파일시스템에 저장하고, 이 객체는 dispatch_photos 테이블의 메타데이터 행을 표현한다.
 */
public class DispatchPhoto {

    private Long id;
    private Long recordId;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;

    public DispatchPhoto(Long recordId, Attachment attachment) {
        this.recordId = recordId;
        this.fileName = attachment.getFileName();
        this.filePath = attachment.getFilePath();
        this.fileSize = attachment.getFileSize();
        this.mimeType = attachment.getMimeType();
        this.uploadedAt = attachment.getUploadedAt();
    }

    public DispatchPhoto(Long id, Long recordId, String fileName, String filePath,
                         long fileSize, String mimeType, LocalDateTime uploadedAt) {
        this.id = id;
        this.recordId = recordId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
