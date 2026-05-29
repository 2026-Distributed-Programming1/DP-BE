package org.dpbe.dao;

import org.dpbe.db.DBA;
import org.dpbe.education.EducationPreparation;
import java.util.ArrayList;
import java.util.List;

public class EducationPreparationDAO {

    public static void save(EducationPreparation e) {
        String attendanceStr = e.getAttendanceList().stream()
                .map(org.dpbe.education.Attendance::getAttendeeName)
                .collect(java.util.stream.Collectors.joining(","));
        DBA.executeUpdate(
            "INSERT INTO education_preparations"
            + " (prep_no, plan_no, trainer_name, venue, material_ready,"
            + "  textbook_status, attendance_list, status, registered_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE"
            + "  venue=VALUES(venue), material_ready=VALUES(material_ready),"
            + "  textbook_status=VALUES(textbook_status),"
            + "  attendance_list=VALUES(attendance_list), status=VALUES(status),"
            + "  registered_at=VALUES(registered_at),"
            + "  plan_no=VALUES(plan_no), trainer_name=VALUES(trainer_name)",
            String.valueOf(e.getSetupNumber()),
            e.getPlanNo(),
            e.getInstructorName(),
            e.getLocation(),
            e.isMaterialReady(),
            e.getTextbookStatus(),
            attendanceStr,
            e.getStatus(),
            e.getRegisteredAt());
    }

    public static List<EducationPreparation> findAll() {
        return DBA.executeQuery(
            "SELECT prep_no, plan_no, trainer_name, venue,"
            + " material_ready, textbook_status, attendance_list, status, registered_at FROM education_preparations",
            rs -> {
                String attendanceStr = rs.getString("attendance_list");
                List<org.dpbe.education.Attendance> attendees = new ArrayList<>();
                if (attendanceStr != null && !attendanceStr.isEmpty()) {
                    for (String name : attendanceStr.split(",")) {
                        if (!name.trim().isEmpty()) {
                            attendees.add(new org.dpbe.education.Attendance(name.trim()));
                        }
                    }
                }
                EducationPreparation e = new EducationPreparation(
                    rs.getInt("prep_no"),
                    null,
                    rs.getString("venue"),
                    rs.getString("trainer_name"),
                    rs.getString("textbook_status"),
                    null,
                    attendees);
                e.setPlanNo(rs.getString("plan_no"));
                e.setMaterialReady(rs.getBoolean("material_ready"));
                e.setStatus(rs.getString("status"));
                e.setRegisteredAt(rs.getTimestamp("registered_at") != null
                    ? rs.getTimestamp("registered_at").toLocalDateTime() : null);
                return e;
            });
    }
}
