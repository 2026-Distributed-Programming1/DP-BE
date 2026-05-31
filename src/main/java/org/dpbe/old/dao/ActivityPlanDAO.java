package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.domain.sales.entity.ActivityPlan;
import org.dpbe.domain.sales.entity.ScheduleItem;

public class ActivityPlanDAO {

    public static void save(ActivityPlan p) {
        String status = p.getStatus() != null ? p.getStatus().name() : null;
        String insuranceType = p.getProposedInsuranceType() != null
                ? p.getProposedInsuranceType().name() : null;
        DBA.executeUpdate(
            "INSERT INTO activity_plans (plan_no, plan_name, author_name, start_date, end_date,"
            + " target_contract_count, target_contract_amount, target_new_customer,"
            + " proposed_customer_id, proposed_insurance_type, proposal_reason, memo, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), plan_name=VALUES(plan_name),"
            + " start_date=VALUES(start_date), end_date=VALUES(end_date),"
            + " target_contract_count=VALUES(target_contract_count),"
            + " target_contract_amount=VALUES(target_contract_amount),"
            + " proposed_insurance_type=VALUES(proposed_insurance_type),"
            + " target_new_customer=VALUES(target_new_customer),"
            + " proposal_reason=VALUES(proposal_reason), memo=VALUES(memo)",
            p.getPlanNo(),
            p.getPlanName(),
            p.getAuthor(),
            p.getStartDate(),
            p.getEndDate(),
            p.getTargetContractCount() != null ? p.getTargetContractCount() : 0,
            p.getTargetContractAmount() != null ? p.getTargetContractAmount() : 0L,
            p.getTargetNewCustomer() != null ? p.getTargetNewCustomer() : 0,
            p.getProposedCustomerId(),
            insuranceType,
            p.getProposalReason(),
            p.getMemo(),
            status);

        for (ScheduleItem item : p.getSchedules()) {
            String actType = item.getActivityType() != null ? item.getActivityType().name() : null;
            DBA.executeUpdate(
                "INSERT INTO activity_schedule_items (plan_no, customer_id, activity_type,"
                + " activity_datetime, location, memo) VALUES (?,?,?,?,?,?)",
                p.getPlanNo(),
                item.getCustomerId(),
                actType,
                item.getActivityDateTime(),
                item.getLocation(),
                item.getMemo());
        }
    }

    public static List<ActivityPlan> findAll() {
        return DBA.executeQuery(
            "SELECT plan_no, plan_name, author_name, start_date, end_date,"
            + " target_contract_count, target_contract_amount, target_new_customer,"
            + " proposed_customer_id, proposed_insurance_type, proposal_reason, memo, status"
            + " FROM activity_plans",
            rs -> {
                ActivityPlan p = new ActivityPlan();
                p.setPlanNo(rs.getString("plan_no"));
                p.setPlanName(rs.getString("plan_name"));
                p.setAuthor(rs.getString("author_name"));
                java.sql.Date sd = rs.getDate("start_date");
                java.sql.Date ed = rs.getDate("end_date");
                if (sd != null) p.setStartDate(sd.toLocalDate());
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
            });
    }
}