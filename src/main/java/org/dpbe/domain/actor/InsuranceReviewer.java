package org.dpbe.domain.actor;

import org.dpbe.domain.consultation.entity.PolicyApplication;
import org.dpbe.domain.consultation.entity.ReviewResult;
import org.dpbe.domain.consultation.entity.Underwriting;

/**
 * 보험심사자 (InsuranceReviewer)
 */
public class InsuranceReviewer extends Employee {

    /** DB 로딩용 생성자 */
    public InsuranceReviewer(String employeeId, String name, String dept, String position) {
        super(employeeId, name, dept, position);
    }

    public Underwriting startUnderwriting(PolicyApplication application) {
        Underwriting underwriting = new Underwriting();
        underwriting.startReview();
        return underwriting;
    }

    public void deliverReviewResult(ReviewResult result) {
        result.deliver();
    }
}
