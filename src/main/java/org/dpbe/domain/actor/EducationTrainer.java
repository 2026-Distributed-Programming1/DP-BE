package org.dpbe.domain.actor;

import org.dpbe.domain.education.entity.EducationExecution;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.domain.education.entity.EducationPreparation;

/**
 * 영업교육담당자 (EducationTrainer)
 */
public class EducationTrainer extends Employee {

    public EducationTrainer(String name, String contact, String email) {
        super(name, contact, email);
    }

    /** DB 로딩용 생성자 */
    public EducationTrainer(String employeeId, String name, String dept, String position) {
        super(employeeId, name, dept, position);
    }

    public EducationPlan createEducationPlan() {
        return new EducationPlan();
    }

    public EducationPreparation registerEducationPreparation() {
        return new EducationPreparation();
    }

    public EducationExecution conductEducation(EducationPreparation preparation) {
        return new EducationExecution(preparation);
    }
}
