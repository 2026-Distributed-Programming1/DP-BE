package org.dpbe.education;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 교육제반 (EducationPreparation)
 * UC: 교육 제반을 등록한다
 */
public class EducationPreparation {

    private static int sequence = 0;

    private int setupNumber;
    private String planNo;
    private LocalDateTime registeredAt;
    private String location;
    private String instructorName;
    private String textbookStatus;
    private boolean materialReady;
    private String additionalNotice;
    private List<Attendance> attendanceList;
    private String status;


    public EducationPreparation(int setupNumber, LocalDateTime registeredAt, String location,
                                String instructorName, String textbookStatus, String additionalNotice,
                                List<Attendance> attendanceList) {
        this.setupNumber = setupNumber;
        this.registeredAt = registeredAt;
        this.location = location;
        this.instructorName = instructorName;
        this.textbookStatus = textbookStatus;
        this.additionalNotice = additionalNotice;
        this.attendanceList = attendanceList != null ? attendanceList : new ArrayList<>();
    }


    public EducationPreparation() {
        sequence += 1;
        this.setupNumber = sequence;
        this.attendanceList = new ArrayList<>();
    }

    public void enterPreparationInfo(String location, String instructorName, String additionalNotice) {
        this.location = location;
        this.instructorName = instructorName;
        this.additionalNotice = additionalNotice;
    }

    public boolean validateRequiredFields() {
        return location != null && !location.isEmpty()
                && instructorName != null && !instructorName.isEmpty()
                && textbookStatus != null && !textbookStatus.isEmpty()
                && attendanceList != null && !attendanceList.isEmpty();
    }

    public void save() {
        this.registeredAt = LocalDateTime.now();
        this.materialReady = this.textbookStatus != null && !this.textbookStatus.isEmpty();
        this.status = "등록완료";
    }

    public void addAttendee(String attendeeName) {
        attendanceList.add(new Attendance(attendeeName));
    }

    public int getSetupNumber() { return setupNumber; }
    public String getPlanNo() { return planNo; }
    public void setPlanNo(String planNo) { this.planNo = planNo; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public String getLocation() { return location; }
    public String getInstructorName() { return instructorName; }
    public String getTextbookStatus() { return textbookStatus; }
    public void setTextbookStatus(String textbookStatus) { this.textbookStatus = textbookStatus; }
    public boolean isMaterialReady() { return materialReady; }
    public void setMaterialReady(boolean materialReady) { this.materialReady = materialReady; }
    public String getAdditionalNotice() { return additionalNotice; }
    public List<Attendance> getAttendanceList() { return attendanceList; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}