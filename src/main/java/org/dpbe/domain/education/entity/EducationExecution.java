package org.dpbe.domain.education.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교육진행 (EducationExecution)
 * UC: 교육을 진행한다
 */
public class EducationExecution {

    private static int sequence = 0;

    private int completionNumber;
    private LocalDateTime completedAt;
    private int attendanceCount;
    private int totalCount;
    private String memo;
    private String status;

    private final EducationPreparation preparation;

    public EducationExecution(int completionNumber, LocalDateTime completedAt, int attendanceCount,
                              int totalCount, String memo, EducationPreparation preparation) {
        this.completionNumber = completionNumber;
        this.completedAt = completedAt;
        this.attendanceCount = attendanceCount;
        this.totalCount = totalCount;
        this.memo = memo;
        this.preparation = preparation;
    }

    public EducationExecution(EducationPreparation preparation) {
        sequence += 1;
        this.completionNumber = sequence;
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
        this.completedAt = LocalDateTime.now();
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

    public int getCompletionNumber() { return completionNumber; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public int getAttendanceCount() { return attendanceCount; }
    public int getTotalCount() { return totalCount; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public EducationPreparation getPreparation() { return preparation; }
}
