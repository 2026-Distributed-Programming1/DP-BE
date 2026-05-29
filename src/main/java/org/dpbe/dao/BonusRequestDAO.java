package org.dpbe.dao;

import org.dpbe.db.DBA;
import org.dpbe.sales.BonusRequest;

public class BonusRequestDAO {

    public static void save(BonusRequest r) {
        String channelType = r.getChannelType() != null ? r.getChannelType().name() : null;
        String grade = r.getEvaluationGrade() != null ? r.getEvaluationGrade().name() : null;
        DBA.executeUpdate(
            "INSERT INTO bonus_requests"
            + " (request_no, requester, evaluation_no, channel_type, evaluation_grade,"
            + "  amount, reason, status, created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE amount=VALUES(amount),"
            + "  evaluation_no=VALUES(evaluation_no),"
            + "  channel_type=VALUES(channel_type),"
            + "  evaluation_grade=VALUES(evaluation_grade)",
            r.getRequestNo(),
            r.getChannelName(),
            r.getEvaluationNo(),
            channelType,
            grade,
            r.getBonusAmount() != null ? r.getBonusAmount().longValue() : 0L,
            r.getRequestReason(),
            "SUBMITTED",
            r.getRequestedAt());
    }
}