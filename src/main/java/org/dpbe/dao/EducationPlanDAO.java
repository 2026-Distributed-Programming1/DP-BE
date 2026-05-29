package org.dpbe.dao;

import org.dpbe.db.DBA;
import org.dpbe.education.EducationPlan;
import java.util.List;

public class EducationPlanDAO {

    public static void save(EducationPlan p) {
        DBA.executeUpdate(
            "INSERT INTO education_plans"
            + " (plan_no, trainer_name, title, target_audience, scheduled_date,"
            + "  end_date, target_count, budget,"
            + "  education_goal, education_content, textbook_list, reject_reason, approved_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE"
            + "  status=VALUES(status), end_date=VALUES(end_date),"
            + "  target_count=VALUES(target_count), budget=VALUES(budget),"
            + "  education_goal=VALUES(education_goal),"
            + "  education_content=VALUES(education_content),"
            + "  textbook_list=VALUES(textbook_list),"
            + "  reject_reason=VALUES(reject_reason),"
            + "  approved_at=VALUES(approved_at),"
            + "  trainer_name=VALUES(trainer_name), title=VALUES(title),"
            + "  target_audience=VALUES(target_audience), scheduled_date=VALUES(scheduled_date)",
            String.valueOf(p.getPlanNumber()),
            p.getTrainerName(),
            p.getEducationName(),
            p.getChannelType(),
            p.getStartDate(),
            p.getEndDate(),
            p.getTargetCount(),
            p.getBudget(),
            p.getEducationGoal(),
            p.getEducationContent(),
            p.getTextbookList(),
            p.getRejectReason(),
            p.getApprovedAt(),
            p.getStatus());
    }

    public static List<EducationPlan> findAll() {
        return DBA.executeQuery(
            "SELECT plan_no, trainer_name, title, target_audience,"
            + " scheduled_date, end_date, target_count, budget,"
            + " education_goal, education_content, textbook_list, reject_reason, approved_at, status"
            + " FROM education_plans",
            rs -> {
                String planNo = rs.getString("plan_no");
                int planNumber = 0;
                if (planNo != null) {
                    try { planNumber = Integer.parseInt(planNo); }
                    catch (NumberFormatException ignored) {}
                }
                java.sql.Date sd = rs.getDate("scheduled_date");
                java.time.LocalDate startDate = sd != null ? sd.toLocalDate() : null;
                java.sql.Date ed = rs.getDate("end_date");
                java.time.LocalDate endDate = ed != null ? ed.toLocalDate() : null;
                EducationPlan plan = EducationPlan.fromDb(
                        planNumber,
                        rs.getString("trainer_name"),
                        rs.getString("title"),
                        rs.getString("target_audience"),
                        startDate,
                        endDate,
                        rs.getInt("target_count"),
                        rs.getLong("budget"),
                        rs.getString("education_goal"),
                        rs.getString("education_content"),
                        rs.getString("textbook_list"),
                        rs.getString("reject_reason"),
                        rs.getString("status"));
                java.sql.Timestamp aat = rs.getTimestamp("approved_at");
                if (aat != null) plan.setApprovedAt(aat.toLocalDateTime());
                return plan;
            });
    }
}
