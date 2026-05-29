package org.dpbe.dao;

import java.util.List;
import org.dpbe.db.DBA;
import org.dpbe.enums.EvaluationGrade;
import org.dpbe.sales.SalesOrgEvaluation;

public class SalesOrgEvaluationDAO {

    public static void save(SalesOrgEvaluation e) {
        String grade = e.getEvaluationGrade() != null ? e.getEvaluationGrade().name() : null;
        String channelType = e.getChannelType() != null ? e.getChannelType().name() : null;
        DBA.executeUpdate(
            "INSERT INTO sales_org_evaluations"
            + " (evaluation_no, org_name, channel_type, grade, score,"
            + "  sales_result, contract_count, evaluation_comment, evaluated_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE org_name=VALUES(org_name),"
            + "  grade=VALUES(grade),"
            + "  channel_type=VALUES(channel_type),"
            + "  sales_result=VALUES(sales_result),"
            + "  contract_count=VALUES(contract_count),"
            + "  evaluation_comment=VALUES(evaluation_comment),"
            + "  score=VALUES(score), evaluated_at=VALUES(evaluated_at)",
            e.getEvaluationNo(),
            e.getChannelName(),
            channelType,
            grade,
            e.getAchievementRate() != null ? e.getAchievementRate() : 0.0,
            e.getSalesResult() != null ? e.getSalesResult() : 0L,
            e.getContractCount() != null ? e.getContractCount() : 0,
            e.getEvaluationComment(),
            e.getEvaluatedAt());
    }

    public static List<SalesOrgEvaluation> findAll() {
        return DBA.executeQuery(
            "SELECT evaluation_no, org_name, channel_type, grade, score,"
            + " sales_result, contract_count, evaluation_comment, evaluated_at"
            + " FROM sales_org_evaluations",
            rs -> {
                SalesOrgEvaluation e = new SalesOrgEvaluation();
                e.setEvaluationNo(rs.getString("evaluation_no"));
                e.setChannelName(rs.getString("org_name"));
                String ct = rs.getString("channel_type");
                if (ct != null) {
                    try { e.setChannelType(org.dpbe.enums.ChannelType.valueOf(ct)); }
                    catch (IllegalArgumentException ignored) {}
                }
                String grade = rs.getString("grade");
                if (grade != null) {
                    try { e.setEvaluationGrade(EvaluationGrade.valueOf(grade)); }
                    catch (IllegalArgumentException ignored) {}
                }
                e.setAchievementRate(rs.getDouble("score"));
                e.setSalesResult(rs.getLong("sales_result"));
                e.setContractCount(rs.getInt("contract_count"));
                e.setEvaluationComment(rs.getString("evaluation_comment"));
                e.setEvaluatedAt(rs.getTimestamp("evaluated_at") != null
                    ? rs.getTimestamp("evaluated_at").toLocalDateTime() : null);
                return e;
            });
    }
}