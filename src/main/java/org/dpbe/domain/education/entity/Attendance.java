package org.dpbe.domain.education.entity;

/**
 * 출석 (Attendance)
 * EducationExecution 과 Composition 관계
 */
public class Attendance {

    private String attendeeName;
    private boolean isAttended;

    public Attendance(String attendeeName, boolean isAttended) {
        this.attendeeName = attendeeName;
        this.isAttended = isAttended;
    }

    public Attendance(String attendeeName) {
        this.attendeeName = attendeeName;
        this.isAttended = false;
    }

    public void mark(boolean isAttended) {
        this.isAttended = isAttended;
    }

    public String getAttendeeName() { return attendeeName; }
    public boolean isAttended() { return isAttended; }
}