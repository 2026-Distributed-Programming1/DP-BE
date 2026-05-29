package org.dpbe.old.dao;

import org.dpbe.old.db.DBA;
import org.dpbe.domain.payment.entity.OverdueNoticeSetting;

public class OverdueNoticeSettingDAO {

    // DB 컬럼명(max_overdue_count → daysAfterDue, notice_method → messageTemplate)이
    // 도메인 필드명과 다르지만, save/find 양방향이 동일하게 매핑하므로 데이터 왕복은 정상이다.
    public static void save(OverdueNoticeSetting s) {
        DBA.executeUpdate(
            "INSERT INTO overdue_notice_settings (id, max_overdue_count, notice_method, auto_cancel_enabled, saved_at)"
            + " VALUES (1,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE max_overdue_count=VALUES(max_overdue_count),"
            + " notice_method=VALUES(notice_method), auto_cancel_enabled=VALUES(auto_cancel_enabled),"
            + " saved_at=VALUES(saved_at)",
            s.getDaysAfterDue(),
            s.getMessageTemplate(),
            s.isEnabled(),
            s.getSavedAt());
    }

    public static OverdueNoticeSetting find() {
        return DBA.queryOne(
            "SELECT max_overdue_count, notice_method, auto_cancel_enabled, saved_at"
            + " FROM overdue_notice_settings WHERE id=1",
            rs -> {
                OverdueNoticeSetting s = new OverdueNoticeSetting();
                s.setDaysAfterDue(rs.getInt("max_overdue_count"));
                s.setMessageTemplate(rs.getString("notice_method"));
                s.toggleEnabled(rs.getBoolean("auto_cancel_enabled"));
                java.sql.Timestamp sat = rs.getTimestamp("saved_at");
                if (sat != null) s.setSavedAt(sat.toLocalDateTime());
                return s;
            });
    }
}