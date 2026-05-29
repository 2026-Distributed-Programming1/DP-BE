package org.dpbe.dao;

import java.util.List;
import org.dpbe.db.DBA;
import org.dpbe.sales.SalesActivityManagement;

public class SalesActivityManagementDAO {

    public static void save(SalesActivityManagement a) {
        String channelType = a.getChannelType() != null ? a.getChannelType().name() : null;
        DBA.executeUpdate(
            "INSERT INTO sales_activity_managements"
            + " (activity_no, manager_name, channel_name, activity_type,"
            + "  start_date, end_date,"
            + "  visit_count, contract_count, achievement_rate,"
            + "  improvement_content, revised_target, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE channel_name=VALUES(channel_name),"
            + "  manager_name=VALUES(manager_name),"
            + "  activity_type=VALUES(activity_type),"
            + "  start_date=VALUES(start_date), end_date=VALUES(end_date),"
            + "  visit_count=VALUES(visit_count),"
            + "  contract_count=VALUES(contract_count),"
            + "  achievement_rate=VALUES(achievement_rate),"
            + "  improvement_content=VALUES(improvement_content),"
            + "  revised_target=VALUES(revised_target)",
            a.getManagementNo(),
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
    }

    public static List<SalesActivityManagement> findAll() {
        return DBA.executeQuery(
            "SELECT activity_no, manager_name, channel_name, activity_type,"
            + " start_date, end_date,"
            + " visit_count, contract_count, achievement_rate,"
            + " improvement_content, revised_target, created_at"
            + " FROM sales_activity_managements",
            rs -> {
                SalesActivityManagement a = new SalesActivityManagement();
                a.setManagementNo(rs.getString("activity_no"));
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
            });
    }
}