package org.dpbe.actor;

import org.dpbe.consultation.PolicyApplication;
import org.dpbe.consultation.ReviewResult;
import org.dpbe.consultation.Underwriting;

/**
 * 보험심사자 (InsuranceReviewer)
 */
public class InsuranceReviewer extends Employee {

    public InsuranceReviewer(String name, String contact, String email) {
        super(name, contact, email);
    }

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
