package org.dpbe.consultation;

import java.time.LocalDateTime;

/**
 * 상담신청 (ConsultationRequest)
 * UC: 상담을 신청한다
 */
public class ConsultationRequest {

    private static int sequence = 0;

    private int consultationNumber;
    private String type;
    private LocalDateTime scheduledAt;
    private String location;
    private String contact;
    private String content;
    private String status;
    private LocalDateTime receivedAt;
    private LocalDateTime acceptedAt;

    public ConsultationRequest(int consultationNumber, String type, LocalDateTime scheduledAt,
                               String location, String contact, String content, String status) {
        this.consultationNumber = consultationNumber;
        this.type = type;
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.contact = contact;
        this.content = content;
        this.status = status;
    }

    public ConsultationRequest() {
        sequence += 1;
        this.consultationNumber = sequence;
        this.status = "접수";
    }

    public void selectType(String type) {
        this.type = type;
    }

    public void enterConsultationInfo(LocalDateTime scheduledAt, String location, String contact, String content) {
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.contact = contact;
        this.content = content;
    }

    public boolean validateRequiredFields() {
        return type != null && !type.isEmpty()
                && contact != null && !contact.isEmpty()
                && content != null && !content.isEmpty();
    }

    public void submit() {
        this.status = "접수";
        this.receivedAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = "수락";
        this.acceptedAt = LocalDateTime.now();
    }

    public int getConsultationNumber() { return consultationNumber; }
    public String getType() { return type; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getLocation() { return location; }
    public String getContact() { return contact; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}