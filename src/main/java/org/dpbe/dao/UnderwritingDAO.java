package org.dpbe.dao;

import org.dpbe.consultation.Underwriting;
import org.dpbe.db.DBA;

public class UnderwritingDAO {

    public static void save(Underwriting u) {
        String result = u.getReviewResult() != null ? u.getReviewResult().getResult() : null;
        DBA.executeUpdate(
            "INSERT INTO underwritings (underwriting_no, app_type, app_no, customer_name,"
            + " risk_grade, review_opinion, result, reviewed_at)"
            + " VALUES (?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE result=VALUES(result),"
            + " risk_grade=VALUES(risk_grade), review_opinion=VALUES(review_opinion),"
            + " reviewed_at=VALUES(reviewed_at)",
            String.valueOf(u.getReviewNumber()),
            u.getReviewType(),
            u.getAppNo(),
            u.getCustomerName(),
            u.getRiskGrade(),
            u.getReviewOpinion(),
            result,
            u.getReviewedAt());
    }
}