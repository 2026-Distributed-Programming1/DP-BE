package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.ActivityType;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.domain.sales.entity.ActivityPlan;
import org.dpbe.domain.sales.entity.ScheduleItem;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityPlanRepository {

    private static final String PLAN_COLS =
            "id, plan_name, author_name, start_date, end_date,"
            + " target_contract_count, target_contract_amount, target_new_customer,"
            + " proposed_customer_id, proposed_insurance_type, proposal_reason, memo, status";

    private final SqlExecutor sql;

    public ActivityPlanRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<ActivityPlan> findAll() {
        return sql.executeQuery(
                "SELECT " + PLAN_COLS + " FROM activity_plans ORDER BY id DESC",
                this::mapPlan);
    }

    public int countAll() {
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM activity_plans",
                rs -> rs.getInt("cnt"));
    }

    public List<ActivityPlan> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + PLAN_COLS + " FROM activity_plans ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapPlan, limit, offset);
    }

    public ActivityPlan findById(Long id) {
        ActivityPlan plan = sql.queryOne(
                "SELECT " + PLAN_COLS + " FROM activity_plans WHERE id=?",
                this::mapPlan, id);
        if (plan != null) {
            loadSchedules(plan);
        }
        return plan;
    }

    public void save(ActivityPlan p) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO activity_plans"
                + " (plan_name, author_name, start_date, end_date,"
                + "  target_contract_count, target_contract_amount, target_new_customer,"
                + "  proposed_customer_id, proposed_insurance_type, proposal_reason, memo, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                p.getPlanName(),
                p.getAuthor(),
                p.getStartDate(),
                p.getEndDate(),
                p.getTargetContractCount() != null ? p.getTargetContractCount() : 0,
                p.getTargetContractAmount() != null ? p.getTargetContractAmount() : 0L,
                p.getTargetNewCustomer() != null ? p.getTargetNewCustomer() : 0,
                p.getProposedCustomerId(),
                p.getProposedInsuranceType() != null ? p.getProposedInsuranceType().name() : null,
                p.getProposalReason(),
                p.getMemo(),
                p.getStatus() != null ? p.getStatus().name() : null);
        p.setId(id);
        p.setPlanNo("APL" + String.format("%05d", id));

        for (ScheduleItem item : p.getSchedules()) {
            sql.executeUpdate(
                    "INSERT INTO activity_schedule_items"
                    + " (plan_id, customer_id, activity_type, activity_datetime, location, memo)"
                    + " VALUES (?,?,?,?,?,?)",
                    p.getId(),
                    item.getCustomerId(),
                    item.getActivityType() != null ? item.getActivityType().name() : null,
                    item.getActivityDateTime(),
                    item.getLocation(),
                    item.getMemo());
        }
    }

    private void loadSchedules(ActivityPlan plan) {
        List<ScheduleItem> items = sql.executeQuery(
                "SELECT customer_id, activity_type, activity_datetime, location, memo"
                + " FROM activity_schedule_items WHERE plan_id=? ORDER BY id",
                rs -> mapScheduleItem(rs), plan.getId());
        items.forEach(plan::addSchedule);
    }

    private ActivityPlan mapPlan(ResultSet rs) throws SQLException {
        ActivityPlan p = new ActivityPlan();
        p.setId(rs.getLong("id"));
        p.setPlanNo("APL" + String.format("%05d", rs.getLong("id")));
        p.setPlanName(rs.getString("plan_name"));
        p.setAuthor(rs.getString("author_name"));
        java.sql.Date sd = rs.getDate("start_date");
        if (sd != null) p.setStartDate(sd.toLocalDate());
        java.sql.Date ed = rs.getDate("end_date");
        if (ed != null) p.setEndDate(ed.toLocalDate());
        p.setTargetContractCount(rs.getInt("target_contract_count"));
        p.setTargetContractAmount(rs.getLong("target_contract_amount"));
        p.setTargetNewCustomer(rs.getInt("target_new_customer"));
        p.setProposedCustomerId(rs.getString("proposed_customer_id"));
        String it = rs.getString("proposed_insurance_type");
        if (it != null) {
            try { p.setProposedInsuranceType(InsuranceType.valueOf(it)); }
            catch (IllegalArgumentException ignored) {}
        }
        p.setProposalReason(rs.getString("proposal_reason"));
        p.setMemo(rs.getString("memo"));
        String st = rs.getString("status");
        if (st != null) {
            try { p.setStatus(PlanStatus.valueOf(st)); }
            catch (IllegalArgumentException ignored) {}
        }
        return p;
    }

    private ScheduleItem mapScheduleItem(ResultSet rs) throws SQLException {
        String at = rs.getString("activity_type");
        ActivityType activityType = null;
        if (at != null) {
            try { activityType = ActivityType.valueOf(at); }
            catch (IllegalArgumentException ignored) {}
        }
        java.sql.Timestamp ts = rs.getTimestamp("activity_datetime");
        return new ScheduleItem(
                rs.getString("customer_id"),
                activityType,
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("location"),
                rs.getString("memo"));
    }
}
