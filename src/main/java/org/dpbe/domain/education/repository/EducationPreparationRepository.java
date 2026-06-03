package org.dpbe.domain.education.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class EducationPreparationRepository {

    private static final String COLS =
            "id, plan_id, instructor_name, venue, material_ready,"
            + " textbook_status, attendance_list, additional_notice, status, registered_at";

    private final SqlExecutor sql;

    public EducationPreparationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<EducationPreparation> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_preparations ORDER BY id DESC", this::mapRow);
    }

    public List<EducationPreparation> findByPlanNo(String planNo) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_preparations WHERE plan_id=? ORDER BY id DESC",
                this::mapRow, parseId(planNo));
    }

    public int countByPlanNo(String planNo) {
        if (planNo != null && !planNo.isBlank()) {
            return sql.queryOne(
                    "SELECT COUNT(*) AS cnt FROM education_preparations WHERE plan_id=?",
                    rs -> rs.getInt("cnt"), parseId(planNo));
        }
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM education_preparations",
                rs -> rs.getInt("cnt"));
    }

    public List<EducationPreparation> findPageByPlanNo(String planNo, int limit, int offset) {
        if (planNo != null && !planNo.isBlank()) {
            return sql.executeQuery(
                    "SELECT " + COLS + " FROM education_preparations WHERE plan_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                    this::mapRow, parseId(planNo), limit, offset);
        }
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_preparations ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public EducationPreparation findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM education_preparations WHERE id=?", this::mapRow, id);
    }

    /** INSERT → id 회수 → prep_no 파생 */
    public void save(EducationPreparation prep) {
        String attendanceStr = prep.getAttendanceList().stream()
                .map(Attendance::getAttendeeName)
                .collect(Collectors.joining(","));
        long id = sql.executeInsertReturningKey(
                "INSERT INTO education_preparations"
                + " (plan_id, instructor_name, venue, material_ready, textbook_status,"
                + "  attendance_list, additional_notice, status, registered_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                parseId(prep.getPlanNo()), prep.getInstructorName(), prep.getVenue(),
                prep.isMaterialReady(), prep.getTextbookStatus(),
                attendanceStr, prep.getAdditionalNotice(),
                prep.getStatus(), prep.getRegisteredAt());
        prep.setId(id);
        prep.setPrepNo("PRP" + String.format("%05d", id));
    }

    private EducationPreparation mapRow(ResultSet rs) throws SQLException {
        String attendanceStr = rs.getString("attendance_list");
        List<Attendance> attendees = new ArrayList<>();
        if (attendanceStr != null && !attendanceStr.isBlank()) {
            for (String name : attendanceStr.split(",")) {
                if (!name.trim().isEmpty()) attendees.add(new Attendance(name.trim()));
            }
        }
        EducationPreparation prep = new EducationPreparation(
                0, null,
                rs.getString("venue"),
                rs.getString("instructor_name"),
                rs.getString("textbook_status"),
                rs.getString("additional_notice"),
                attendees);
        prep.setId(rs.getLong("id"));
        prep.setPrepNo("PRP" + String.format("%05d", rs.getLong("id")));
        long planId = rs.getLong("plan_id");
        if (!rs.wasNull()) prep.setPlanNo("PLN" + String.format("%05d", planId));
        prep.setMaterialReady(rs.getBoolean("material_ready"));
        prep.setStatus(rs.getString("status"));
        java.sql.Timestamp rat = rs.getTimestamp("registered_at");
        if (rat != null) prep.setRegisteredAt(rat.toLocalDateTime());
        return prep;
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }
}
