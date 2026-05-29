package org.dpbe;

import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.old.runner.SampleData;
import org.dpbe.old.runner.usecase.AccidentReportRunner;
import org.dpbe.old.runner.usecase.ActivityPlanRunner;
import org.dpbe.old.runner.usecase.ChannelScreeningRunner;
import org.dpbe.old.runner.usecase.ClaimRequestRunner;
import org.dpbe.old.runner.usecase.ConsultationRequestRunner;
import org.dpbe.old.runner.usecase.ContractInfoRunner;
import org.dpbe.old.runner.usecase.CustomerRegistrationRunner;
import org.dpbe.old.runner.usecase.DamageInvestigationRunner;
import org.dpbe.old.runner.usecase.DispatchRecordRunner;
import org.dpbe.old.runner.usecase.EducationPlanRunner;
import org.dpbe.old.runner.usecase.EducationPreparationRunner;
import org.dpbe.old.runner.usecase.InquiryRunner;
import org.dpbe.old.runner.usecase.InsuranceProductInquiryRunner;
import org.dpbe.old.runner.usecase.InterviewRecordRunner;
import org.dpbe.old.runner.usecase.InterviewScheduleRunner;
import org.dpbe.old.runner.usecase.MyInsuranceViewRunner;
import org.dpbe.old.runner.usecase.PaymentRecordRunner;
import org.dpbe.old.runner.usecase.PolicyApplicationRunner;
import org.dpbe.old.runner.usecase.RefundListRunner;
import org.dpbe.old.runner.usecase.SalesActivityRunner;
import org.dpbe.old.runner.usecase.SalesOrgEvaluationRunner;
import org.dpbe.old.runner.usecase.UnderwritingRunner;

/**
 * 인터랙티브 메인 진입점
 *
 * Runner는 Usecase Scenario, Usecase Diagram을 참고
 * Main이 Usecase Diagram 참고
 * 다른 클래스들은 Class Diagram 참고
 */
public class OldMain {

    public static void main(String[] args) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("  보험 시스템 시나리오 시뮬레이터");
        ConsoleHelper.printDoubleDivider();

        SampleData.initialize();

        while (true) {
            int roleChoice = ConsoleHelper.readMenuChoice(
                    "\n역할을 선택하세요:",
                    "고객",
                    "현장출동 직원 / 손해사정사",
                    "재무회계 담당자",
                    "영업교육 담당자",
                    "판매채널 (설계사·대리점)",
                    "영업 관리자",
                    "보험 심사자",
                    "계약 관리 담당자",
                    "종료");

            switch (roleChoice) {
                case 1: customerMenu(); break;
                case 2: claimsHandlerMenu(); break;
                case 3: financeMenu(); break;
                case 4: educationMenu(); break;
                case 5: salesChannelMenu(); break;
                case 6: salesManagerMenu(); break;
                case 7: reviewerMenu(); break;
                case 8: contractMenu(); break;
                case 9:
                    System.out.println("\n프로그램을 종료합니다.");
                    return;
            }
        }
    }

    /** 고객: 상담 요청, 보험상품 조회, 사고 접수, 보험금 요청, 가입 보험 조회, 문의 */
    private static void customerMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[고객] 메뉴를 선택하세요:",
                    "상담을 요청한다",
                    "보험상품을 조회한다  (→ 신청 · 부활 연결)",
                    "사고를 접수한다",
                    "보험금을 요청한다",
                    "가입 보험을 조회한다  (→ 납입 · 해지 연결)",
                    "문의한다",
                    "뒤로");
            switch (choice) {
                case 1: ConsultationRequestRunner.run(); break;
                case 2: InsuranceProductInquiryRunner.run(); break;
                case 3: AccidentReportRunner.run(); break;
                case 4: ClaimRequestRunner.run(); break;
                case 5: MyInsuranceViewRunner.run(); break;
                case 6: InquiryRunner.run(); break;
                case 7: return;
            }
        }
    }

    /** 현장출동 직원 / 손해사정사: 출동 기록, 손해 조사 (→ 보험금 산출·지급 연결) */
    private static void claimsHandlerMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[현장출동 직원 / 손해사정사] 메뉴를 선택하세요:",
                    "현장 출동 정보를 기록한다",
                    "손해를 조사한다  (→ 보험금 산출 · 지급 연결)",
                    "뒤로");
            switch (choice) {
                case 1: DispatchRecordRunner.run(); break;
                case 2: DamageInvestigationRunner.run(); break;
                case 3: return;
            }
        }
    }

    /** 재무회계 담당자: 납부 내역 관리, 해약 환급 내역 조회 (→ 산출·지급 포함) */
    private static void financeMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[재무회계 담당자] 메뉴를 선택하세요:",
                    "보험료 납부 내역을 관리한다",
                    "해약 환급 내역을 조회한다  (→ 산출 · 지급 포함)",
                    "뒤로");
            switch (choice) {
                case 1: PaymentRecordRunner.run(); break;
                case 2: RefundListRunner.run(); break;
                case 3: return;
            }
        }
    }

    /** 영업교육 담당자: 교육 계획안 작성, 교육 제반 등록 (→ 교육 진행 포함) */
    private static void educationMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[영업교육 담당자] 메뉴를 선택하세요:",
                    "교육 계획안을 작성한다",
                    "교육 제반을 등록한다  (→ 교육 진행 포함)",
                    "뒤로");
            switch (choice) {
                case 1: EducationPlanRunner.run(); break;
                case 2: EducationPreparationRunner.run(); break;
                case 3: return;
            }
        }
    }

    /** 판매채널(설계사·대리점): 면담일정·면담기록 관리, 청약서 작성, 고객 등록, 활동 계획 */
    private static void salesChannelMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[판매채널] 메뉴를 선택하세요:",
                    "면담일정을 관리한다",
                    "면담기록을 관리한다  (→ 보험상품 제안 포함)",
                    "청약서를 작성한다",
                    "고객 정보를 등록한다",
                    "활동 계획을 작성한다",
                    "뒤로");
            switch (choice) {
                case 1: InterviewScheduleRunner.run(); break;
                case 2: InterviewRecordRunner.run(); break;
                case 3: PolicyApplicationRunner.run(); break;
                case 4: CustomerRegistrationRunner.run(); break;
                case 5: ActivityPlanRunner.run(); break;
                case 6: return;
            }
        }
    }

    /** 영업 관리자: 영업 활동 관리, 채용 심사, 영업조직 평가 */
    private static void salesManagerMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[영업 관리자] 메뉴를 선택하세요:",
                    "영업 활동을 관리한다  (→ 판매채널 모집 연결)",
                    "판매채널 채용을 심사한다",
                    "영업조직을 평가한다  (→ 성과급 요청 연결)",
                    "뒤로");
            switch (choice) {
                case 1: SalesActivityRunner.run(); break;
                case 2: ChannelScreeningRunner.run(); break;
                case 3: SalesOrgEvaluationRunner.run(); break;
                case 4: return;
            }
        }
    }

    /** 보험 심사자: 인수 심사 (→ 심사 결과 전달 포함) */
    private static void reviewerMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[보험 심사자] 메뉴를 선택하세요:",
                    "인수 심사를 한다  (→ 심사 결과 전달 포함)",
                    "뒤로");
            switch (choice) {
                case 1: UnderwritingRunner.run(); break;
                case 2: return;
            }
        }
    }

    /** 계약 관리 담당자: 계약 정보 조회 (→ 통계·만기 관리 연결) */
    private static void contractMenu() {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[계약 관리 담당자] 메뉴를 선택하세요:",
                    "계약 정보를 조회한다  (→ 통계 · 만기 관리 연결)",
                    "뒤로");
            switch (choice) {
                case 1: ContractInfoRunner.run(); break;
                case 2: return;
            }
        }
    }
}