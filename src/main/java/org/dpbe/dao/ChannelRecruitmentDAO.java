package org.dpbe.dao;

import org.dpbe.db.DBA;
import org.dpbe.enums.ChannelType;
import org.dpbe.sales.ChannelRecruitment;
import java.util.List;

public class ChannelRecruitmentDAO {

    public static void save(ChannelRecruitment r) {
        String channelType = r.getChannelType() != null ? r.getChannelType().name() : null;
        DBA.executeUpdate(
            "INSERT INTO channel_recruitments (recruitment_no, manager_name, channel_type,"
            + " recruit_count, start_date, end_date, condition_text, status, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE manager_name=VALUES(manager_name),"
            + " channel_type=VALUES(channel_type),"
            + " recruit_count=VALUES(recruit_count), start_date=VALUES(start_date),"
            + " end_date=VALUES(end_date), condition_text=VALUES(condition_text)",
            r.getRecruitmentNo(),
            r.getManagerName(),
            channelType,
            r.getRecruitCount(),
            r.getLocalStartDate(),
            r.getLocalEndDate(),
            r.getCondition(),
            null,
            r.getRegisteredAt());
    }

    public static List<ChannelRecruitment> findAll() {
        return DBA.executeQuery(
            "SELECT recruitment_no, channel_type, manager_name, recruit_count,"
            + " start_date, end_date, condition_text, created_at FROM channel_recruitments",
            rs -> {
                String ct = rs.getString("channel_type");
                ChannelType channelType = null;
                if (ct != null) {
                    try { channelType = ChannelType.valueOf(ct); }
                    catch (IllegalArgumentException ignored) {}
                }
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                java.time.LocalDateTime registeredAt = ts != null ? ts.toLocalDateTime() : null;
                ChannelRecruitment r = ChannelRecruitment.fromDb(
                        rs.getString("recruitment_no"),
                        channelType,
                        rs.getString("manager_name"),
                        registeredAt);
                r.setRecruitCount(rs.getInt("recruit_count"));
                java.sql.Date sd = rs.getDate("start_date");
                if (sd != null) r.setLocalStartDate(sd.toLocalDate());
                java.sql.Date ed = rs.getDate("end_date");
                if (ed != null) r.setLocalEndDate(ed.toLocalDate());
                r.setCondition(rs.getString("condition_text"));
                return r;
            });
    }
}