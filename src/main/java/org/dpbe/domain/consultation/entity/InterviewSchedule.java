package org.dpbe.domain.consultation.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.global.exception.ApiException;

/**
 * 면담일정 (InterviewSchedule)
 * UC: 면담 일정을 정한다
 */
public class InterviewSchedule {

    private Long id;
    private String scheduleNo;
    private int interviewNumber;
    private String customerName;
    private String designerName;
    private String type;
    private LocalDateTime scheduledAt;
    private String location;
    private String preparation;
    private String status;
    private LocalDateTime registeredAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime cancelledAt;
    private List<InterviewRecord> interviewRecordList;

    public InterviewSchedule(int interviewNumber, String customerName, String type, LocalDateTime scheduledAt,
                             String location, String preparation, String status, List<InterviewRecord> interviewRecordList) {
        this.interviewNumber = interviewNumber;
        this.customerName = customerName;
        this.type = type;
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.preparation = preparation;
        this.status = status;
        this.interviewRecordList = interviewRecordList != null ? interviewRecordList : new ArrayList<>();
    }
  
    private InterviewSchedule(boolean fromDb) {
        this.interviewRecordList = new ArrayList<>();
    }

    public static InterviewSchedule fromDb(int interviewNumber, String customerName,
                                            String type, LocalDateTime scheduledAt,
                                            String location, String preparation, String status,
                                            LocalDateTime registeredAt, LocalDateTime modifiedAt,
                                            LocalDateTime cancelledAt) {
        InterviewSchedule s = new InterviewSchedule(true);
        s.interviewNumber = interviewNumber;
        s.customerName    = customerName;
        s.type            = type;
        s.scheduledAt     = scheduledAt;
        s.location        = location;
        s.preparation     = preparation;
        s.status          = status;
        s.registeredAt    = registeredAt;
        s.modifiedAt      = modifiedAt;
        s.cancelledAt     = cancelledAt;
        return s;
    }

    public InterviewSchedule() {
        this.interviewRecordList = new ArrayList<>();
        this.status = "예정";
    }

    public void register(String customerName, LocalDateTime scheduledAt, String location, String preparation) {
        this.customerName = customerName;
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.preparation = preparation;
        this.status = "예정";
        this.registeredAt = LocalDateTime.now();
    }

    public void modify(LocalDateTime scheduledAt, String location, String preparation) {
        if ("취소".equals(this.status)) {
            throw ApiException.badRequest("취소된 면담일정은 수정할 수 없습니다.");
        }
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.preparation = preparation;
        this.modifiedAt = LocalDateTime.now();
    }

    public void cancel() {
        if ("취소".equals(this.status)) {
            throw ApiException.badRequest("이미 취소된 면담일정입니다.");
        }
        this.status = "취소";
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean validateRequiredFields() {
        return customerName != null && !customerName.isEmpty() && scheduledAt != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScheduleNo() { return scheduleNo; }
    public void setScheduleNo(String scheduleNo) { this.scheduleNo = scheduleNo; }
    public int getInterviewNumber() { return interviewNumber; }
    public String getCustomerName() { return customerName; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getLocation() { return location; }
    public String getPreparation() { return preparation; }
    public String getStatus() { return status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<InterviewRecord> getInterviewRecordList() { return interviewRecordList; }
}