package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.sales.entity.ChannelRecruitment;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelRecruitmentRepository {

    private static final String COLS =
            "id, manager_name, channel_type, recruit_count,"
            + " start_date, end_date, condition_text, status, created_at";

    private final SqlExecutor sql;

    public ChannelRecruitmentRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<ChannelRecruitment> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM channel_recruitments ORDER BY id DESC",
                this::mapRow);
    }

    public int countAll() {
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM channel_recruitments",
                rs -> rs.getInt("cnt"));
    }

    public List<ChannelRecruitment> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM channel_recruitments ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public void save(ChannelRecruitment r) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO channel_recruitments"
                + " (manager_name, channel_type, recruit_count, start_date, end_date,"
                + "  condition_text, status, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                r.getManagerName(),
                r.getChannelType() != null ? r.getChannelType().name() : null,
                r.getRecruitCount(),
                r.getStartDate(),
                r.getEndDate(),
                r.getCondition(),
                r.getStatus(),
                r.getRegisteredAt());
        r.setId(id);
        r.setRecruitmentNo("RCT" + String.format("%05d", id));
    }

    private ChannelRecruitment mapRow(ResultSet rs) throws SQLException {
        ChannelRecruitment r = new ChannelRecruitment();
        r.setId(rs.getLong("id"));
        r.setRecruitmentNo("RCT" + String.format("%05d", rs.getLong("id")));
        r.setManagerName(rs.getString("manager_name"));
        String ct = rs.getString("channel_type");
        if (ct != null) {
            try { r.setChannelType(ChannelType.valueOf(ct)); }
            catch (IllegalArgumentException ignored) {}
        }
        r.setRecruitCount(rs.getInt("recruit_count"));
        java.sql.Date sd = rs.getDate("start_date");
        if (sd != null) r.setStartDate(sd.toLocalDate());
        java.sql.Date ed = rs.getDate("end_date");
        if (ed != null) r.setEndDate(ed.toLocalDate());
        r.setCondition(rs.getString("condition_text"));
        r.setStatus(rs.getString("status"));
        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setRegisteredAt(ts.toLocalDateTime());
        return r;
    }
}
