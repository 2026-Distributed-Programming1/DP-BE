package org.dpbe.domain.education.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교육진행 (EducationExecution)
 * UC: 교육을 진행한다
 */
public class EducationExecution {

    private Long id;
    private int completionNumber;
    private String executionNo;
    private LocalDateTime executedAt;
    private int attendanceCount;
    private int totalCount;
    private String memo;
    private String status;

    private final EducationPreparation preparation;

    public EducationExecution(int completionNumber, LocalDateTime executedAt, int attendanceCount,
                              int totalCount, String memo, EducationPreparation preparation) {
        this.completionNumber = completionNumber;
        this.executedAt = executedAt;
        this.attendanceCount = attendanceCount;
        this.totalCount = totalCount;
        this.memo = memo;
        this.preparation = preparation;
    }

    public EducationExecution(EducationPreparation preparation) {
        this.preparation = preparation;
    }

    public List<Attendance> loadAttendanceList() {
        return preparation.getAttendanceList();
    }

    public void markAttendance(String attendeeName, boolean isAttended) {
        for (Attendance a : preparation.getAttendanceList()) {
            if (a.getAttendeeName().equals(attendeeName)) {
                a.mark(isAttended);
                break;
            }
        }
    }

    public int calculateAttendanceCount() {
        List<Attendance> list = preparation.getAttendanceList();
        this.totalCount = list.size();
        this.attendanceCount = (int) list.stream().filter(Attendance::isAttended).count();
        return attendanceCount;
    }

    public void complete() {
        this.executedAt = LocalDateTime.now();
        calculateAttendanceCount();
        this.status = "완료";
    }

    public void sendCompletionNotice() {
        System.out.println("  [시스템] 판매채널에 수료 알림이 발송되었습니다.");
        for (Attendance a : preparation.getAttendanceList()) {
            System.out.println("    - " + a.getAttendeeName()
                    + " : " + (a.isAttended() ? "수료" : "미수료"));
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExecutionNo() { return executionNo; }
    public void setExecutionNo(String executionNo) { this.executionNo = executionNo; }
    public int getCompletionNumber() { return completionNumber; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public int getAttendanceCount() { return attendanceCount; }
    public int getTotalCount() { return totalCount; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public EducationPreparation getPreparation() { return preparation; }
}
