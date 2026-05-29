package org.dpbe.domain.common.entity;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 첨부파일 - 공통 (Attachment)
 *
 * 사진, 서류 등 다양한 도메인에서 사용되는 첨부파일 클래스이다.
 */
public class Attachment {

    private static int sequence = 0;     // ID 자동 부여용 카운터

    private String fileId;               // 파일 ID
    private String fileName;             // 파일명
    private long fileSize;               // 파일 크기
    private String filePath;             // 저장 경로
    private String mimeType;             // 파일 형식
    private LocalDateTime uploadedAt;    // 업로드 일시

    /** 생성자 - 파일 ID 자동 부여, 업로드일시=now() */
    public Attachment(File file) {
        sequence += 1;
        this.fileId = "ATT" + String.format("%05d", sequence);
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.filePath = file.getAbsolutePath();
        this.mimeType = guessMimeType(file.getName());
        this.uploadedAt = LocalDateTime.now();
    }

    /**
     * 삭제
     * 외부 파일시스템 연동이 필요한 부분이므로 더미로 처리한다.
     */
    public void delete() {
        System.out.println("[Attachment] 파일 삭제: " + fileName);
    }

    /**
     * 다운로드
     * 외부 파일시스템 연동이 필요한 부분이므로 더미로 처리한다.
     */
    public File download() {
        System.out.println("[Attachment] 파일 다운로드: " + fileName);
        return new File(filePath);
    }

    /** 단순 mime 타입 추정 */
    private String guessMimeType(String name) {
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    // Getter
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getFilePath() { return filePath; }
    public String getMimeType() { return mimeType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
