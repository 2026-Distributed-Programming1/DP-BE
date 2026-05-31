package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class SalesOrgEvaluationRepository {

    private static final String COLS =
            "id, channel_name, channel_type, grade,"
            + " achievement_rate, sales_result, contract_count, evaluation_comment, evaluated_at";

    private final SqlExecutor sql;

    public SalesOrgEvaluationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<SalesOrgEvaluation> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM sales_org_evaluations ORDER BY id DESC",
                this::mapRow);
    }

    public void save(SalesOrgEvaluation e) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO sales_org_evaluations"
                + " (channel_name, channel_type, grade, achievement_rate,"
                + "  sales_result, contract_count, evaluation_comment, evaluated_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                e.getChannelName(),
                e.getChannelType() != null ? e.getChannelType().name() : null,
                e.getEvaluationGrade() != null ? e.getEvaluationGrade().name() : null,
                e.getAchievementRate() != null ? e.getAchievementRate() : 0.0,
                e.getSalesResult() != null ? e.getSalesResult() : 0L,
                e.getContractCount() != null ? e.getContractCount() : 0,
                e.getEvaluationComment(),
                e.getEvaluatedAt());
        e.setId(id);
        e.setEvaluationNo("EVL" + String.format("%05d", id));
    }

    private SalesOrgEvaluation mapRow(ResultSet rs) throws SQLException {
        SalesOrgEvaluation e = new SalesOrgEvaluation();
        e.setId(rs.getLong("id"));
        e.setEvaluationNo("EVL" + String.format("%05d", rs.getLong("id")));
        e.setChannelName(rs.getString("channel_name"));
        String ct = rs.getString("channel_type");
        if (ct != null) {
            try { e.setChannelType(ChannelType.valueOf(ct)); }
            catch (IllegalArgumentException ignored) {}
        }
        String grade = rs.getString("grade");
        if (grade != null) {
            try { e.setEvaluationGrade(EvaluationGrade.valueOf(grade)); }
            catch (IllegalArgumentException ignored) {}
        }
        e.setAchievementRate(rs.getDouble("achievement_rate"));
        e.setSalesResult(rs.getLong("sales_result"));
        e.setContractCount(rs.getInt("contract_count"));
        e.setEvaluationComment(rs.getString("evaluation_comment"));
        java.sql.Timestamp ts = rs.getTimestamp("evaluated_at");
        if (ts != null) e.setEvaluatedAt(ts.toLocalDateTime());
        return e;
    }
}