package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.sales.entity.SalesActivityManagement;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class SalesActivityManagementRepository {

    private static final String COLS =
            "id, manager_name, channel_name, activity_type,"
            + " start_date, end_date, visit_count, contract_count, achievement_rate,"
            + " improvement_content, revised_target, created_at";

    private final SqlExecutor sql;

    public SalesActivityManagementRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<SalesActivityManagement> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM sales_activity_managements ORDER BY id DESC",
                this::mapRow);
    }

    public void save(SalesActivityManagement a) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO sales_activity_managements"
                + " (manager_name, channel_name, activity_type,"
                + "  start_date, end_date, visit_count, contract_count, achievement_rate,"
                + "  improvement_content, revised_target, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                a.getManagerName(),
                a.getChannelName(),
                a.getActivityType(),
                a.getStartDate(),
                a.getEndDate(),
                a.getVisitCount() != null ? a.getVisitCount() : 0,
                a.getContractCount() != null ? a.getContractCount() : 0,
                a.getAchievementRate() != null ? a.getAchievementRate() : 0.0,
                a.getImprovementContent(),
                a.getRevisedTarget() != null ? a.getRevisedTarget() : 0,
                a.getRegisteredAt());
        a.setId(id);
        a.setActivityNo("SAM" + String.format("%05d", id));
    }

    private SalesActivityManagement mapRow(ResultSet rs) throws SQLException {
        SalesActivityManagement a = new SalesActivityManagement();
        a.setId(rs.getLong("id"));
        a.setActivityNo("SAM" + String.format("%05d", rs.getLong("id")));
        a.setManagerName(rs.getString("manager_name"));
        a.setChannelName(rs.getString("channel_name"));
        a.setActivityType(rs.getString("activity_type"));
        java.sql.Date sd = rs.getDate("start_date");
        if (sd != null) a.setStartDate(sd.toLocalDate());
        java.sql.Date ed = rs.getDate("end_date");
        if (ed != null) a.setEndDate(ed.toLocalDate());
        a.setVisitCount(rs.getInt("visit_count"));
        a.setContractCount(rs.getInt("contract_count"));
        a.setAchievementRate(rs.getDouble("achievement_rate"));
        a.setImprovementContent(rs.getString("improvement_content"));
        a.setRevisedTarget(rs.getInt("revised_target"));
        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) a.setRegisteredAt(ts.toLocalDateTime());
        return a;
    }
}