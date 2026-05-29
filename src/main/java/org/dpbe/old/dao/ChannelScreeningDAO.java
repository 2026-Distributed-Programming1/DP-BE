package org.dpbe.old.dao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ScreeningStatus;
import org.dpbe.domain.sales.entity.ChannelScreening;

public class ChannelScreeningDAO {

    public static void save(ChannelScreening s) {
        String screeningNo = s.getApprovalNo() != null ? s.getApprovalNo()
                : "CS-" + (s.getApplicantName() != null ? s.getApplicantName() : "")
                  + "-" + s.getApplicationDate();
        String channelType = s.getChannelType() != null ? s.getChannelType().name() : null;
        String status = s.getScreeningStatus() != null ? s.getScreeningStatus().name() : null;
        String certsStr = s.getCertifications().isEmpty() ? null
                : String.join(",", s.getCertifications());
        DBA.executeUpdate(
            "INSERT INTO channel_screenings (screening_no, candidate_name, channel_type,"
            + " qualification, certifications, application_date, rejection_reason, status, reviewed_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " application_date=VALUES(application_date),"
            + " certifications=VALUES(certifications),"
            + " rejection_reason=VALUES(rejection_reason)",
            screeningNo,
            s.getApplicantName(),
            channelType,
            s.getCareer(),
            certsStr,
            s.getApplicationDate(),
            s.getRejectionReason(),
            status,
            s.getApprovedAt());
    }

    public static List<ChannelScreening> findAll() {
        return DBA.executeQuery(
            "SELECT screening_no, candidate_name, channel_type, qualification,"
            + " certifications, application_date, rejection_reason, status, reviewed_at FROM channel_screenings",
            rs -> {
                ChannelScreening s = new ChannelScreening();
                s.setApprovalNo(rs.getString("screening_no"));
                s.setApplicantName(rs.getString("candidate_name"));
                String ct = rs.getString("channel_type");
                if (ct != null) {
                    try { s.setChannelType(ChannelType.valueOf(ct)); }
                    catch (IllegalArgumentException ignored) {}
                }
                s.setCareer(rs.getString("qualification"));
                String certsStr = rs.getString("certifications");
                if (certsStr != null && !certsStr.isEmpty()) {
                    s.setCertifications(Arrays.stream(certsStr.split(","))
                            .map(String::trim).collect(Collectors.toList()));
                }
                java.sql.Date ad = rs.getDate("application_date");
                if (ad != null) s.setApplicationDate(ad.toLocalDate());
                s.setRejectionReason(rs.getString("rejection_reason"));
                String st = rs.getString("status");
                if (st != null) {
                    try { s.setScreeningStatus(ScreeningStatus.valueOf(st)); }
                    catch (IllegalArgumentException ignored) {}
                }
                java.sql.Timestamp rat = rs.getTimestamp("reviewed_at");
                if (rat != null) s.setApprovedAt(rat.toLocalDateTime());
                return s;
            });
    }
}