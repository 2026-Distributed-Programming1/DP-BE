package org.dpbe.old.runner.usecase;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.actor.Employee;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimPayment;
import org.dpbe.old.dao.ClaimCalculationDAO;
import org.dpbe.old.dao.ClaimPaymentDAO;
import org.dpbe.old.dao.ClaimsHandlerDAO;
import org.dpbe.domain.common.enums.CalculationStatus;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC5: 「보험금을 산출한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 산출 건 선택 → 산출 결과 검토 → (전결 한도 내) 승인 → 지급 이관
 * 분기:
 * - A1: 산출액이 전결 한도 초과 → 결재 상신
 * - A2: 이전 페이지로 이동
 * - E1: 자기부담금 초과 → 종결 처리
 * - E2: 보장 한도 초과 → 한도까지 자동 조정
 */
public class ClaimCalculationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC5: 보험금을 산출한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 보상담당자 선택 (전결 한도 비교용)
        ClaimsHandler handler = selectHandler();
        if (handler == null) return;

        // 2) 산출 대상 선택
        ClaimCalculation calc = selectCalculation();
        if (calc == null) return;

        // 3) 산출 결과 검토
        showCalculation(calc);

        // E1 검사: 자기부담금 초과 여부
        if (calc.isExceededDeductible()) {
            ConsoleHelper.printError("[E1] 적용 손해액이 자기부담금 이하입니다. 지급할 금액이 없습니다.");
            if (ConsoleHelper.readYesNo("  공제액 초과로 종결 처리하시겠습니까?")) {
                calc.closeAsExceeded();
                ClaimCalculationDAO.save(calc);
            }
            ConsoleHelper.waitEnter();
            return;
        }

        // E2 안내: 보장 한도 초과 시 자동 조정 결과
        if (calc.isAdjusted()) {
            ConsoleHelper.printWarning("[E2] 산출액이 보장 한도를 초과하여 한도까지 자동 조정되었습니다.");
            ConsoleHelper.printInfo("  최종 산출액: " + calc.getFinalAmount() + "원");
        }

        // 4) 작업 선택
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[보상담당자] 다음 작업을 선택하세요:",
                    "지급 승인 → 지급 이관 (정상)",
                    "결재 상신 (A1, 전결 한도 초과 시)",
                    "이전 페이지 (A2)");
            if (choice == 1) {
                if (calc.getFinalAmount() > handler.getTransferLimit()) {
                    ConsoleHelper.printError("산출액(" + calc.getFinalAmount()
                            + ")이 전결 한도(" + handler.getTransferLimit()
                            + ")를 초과합니다. 결재 상신을 사용하세요.");
                    continue;
                }
                ClaimPayment payment = calc.approve();
                ClaimCalculationDAO.save(calc);
                if (payment != null) {
                    ClaimPaymentDAO.save(payment);
                    ConsoleHelper.printSuccess("지급 승인 완료, 지급번호: " + payment.getPaymentNo());
                }
                ConsoleHelper.waitEnter();
                return;
            } else if (choice == 2) {
                handleApproval(calc, handler);
                return;
            } else if (choice == 3) {
                return;
            }
        }
    }

    /** UC4에서 이관받아 실행 — 산출 건 선택 없이 직접 처리 후 UC6 이관 (UC4 → UC5 → UC6 체인) */
    static void run(ClaimsHandler handler, ClaimCalculation calc) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC5: 보험금을 산출한다");
        ConsoleHelper.printDoubleDivider();

        showCalculation(calc);

        if (calc.isExceededDeductible()) {
            ConsoleHelper.printError("[E1] 적용 손해액이 자기부담금 이하입니다. 지급할 금액이 없습니다.");
            if (ConsoleHelper.readYesNo("  공제액 초과로 종결 처리하시겠습니까?")) {
                calc.closeAsExceeded();
                ClaimCalculationDAO.save(calc);
            }
            ConsoleHelper.waitEnter();
            return;
        }

        if (calc.isAdjusted()) {
            ConsoleHelper.printWarning("[E2] 산출액이 보장 한도를 초과하여 한도까지 자동 조정되었습니다.");
            ConsoleHelper.printInfo("  최종 산출액: " + calc.getFinalAmount() + "원");
        }

        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[보상담당자] 다음 작업을 선택하세요:",
                    "지급 승인 → 지급 이관 (정상)",
                    "결재 상신 (A1, 전결 한도 초과 시)",
                    "이전 페이지 (A2)");
            if (choice == 1) {
                if (calc.getFinalAmount() > handler.getTransferLimit()) {
                    ConsoleHelper.printError("산출액(" + calc.getFinalAmount()
                            + ")이 전결 한도(" + handler.getTransferLimit()
                            + ")를 초과합니다. 결재 상신을 사용하세요.");
                    continue;
                }
                ClaimPayment payment = calc.approve();
                ClaimCalculationDAO.save(calc);
                if (payment != null) {
                    ClaimPaymentDAO.save(payment);
                    ConsoleHelper.printSuccess("지급 승인 완료, 지급번호: " + payment.getPaymentNo());
                    ConsoleHelper.printInfo("→ 보험금 지급 단계로 이관합니다.");
                    ClaimPaymentRunner.run(payment);
                }
                return;
            } else if (choice == 2) {
                handleApprovalChained(calc, handler);
                return;
            } else if (choice == 3) {
                return;
            }
        }
    }

    private static void handleApprovalChained(ClaimCalculation calc, ClaimsHandler current) {
        ConsoleHelper.printStage("보상담당자", "[A1] 결재 상신을 진행합니다.");

        List<ClaimsHandler> approvers = ClaimsHandlerDAO.findAll().stream()
                .filter(h -> h != current && h.getTransferLimit() >= calc.getFinalAmount())
                .collect(Collectors.toList());
        if (approvers.isEmpty()) {
            ConsoleHelper.printError("결재 가능한 상위 결재권자가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        String[] options = approvers.stream()
                .map(h -> h.getName() + " (" + h.getPosition() + ", 전결한도 " + h.getTransferLimit() + ")")
                .toArray(String[]::new);
        int idx = ConsoleHelper.readMenuChoice("결재권자를 선택하세요:", options);
        Employee approver = approvers.get(idx - 1);
        calc.selectApprover(approver);
        calc.submitForApproval();
        ClaimCalculationDAO.save(calc);
        ConsoleHelper.printSuccess("결재 상신 완료. (시연 단계에서는 자동 승인 처리)");

        if (ConsoleHelper.readYesNo("  결재권자가 승인했다고 가정하고 지급 이관을 진행할까요?")) {
            ClaimPayment payment = calc.approve();
            ClaimCalculationDAO.save(calc);
            if (payment != null) {
                ClaimPaymentDAO.save(payment);
                ConsoleHelper.printSuccess("지급 승인 및 이관 완료, 지급번호: " + payment.getPaymentNo());
                ConsoleHelper.printInfo("→ 보험금 지급 단계로 이관합니다.");
                ClaimPaymentRunner.run(payment);
            }
        }
    }

    /** A1: 결재 상신 (단독 실행용) */
    private static void handleApproval(ClaimCalculation calc, ClaimsHandler current) {
        ConsoleHelper.printStage("보상담당자", "[A1] 결재 상신을 진행합니다.");

        // 전결 한도가 더 큰 다른 보상담당자를 결재선으로
        List<ClaimsHandler> approvers = ClaimsHandlerDAO.findAll().stream()
                .filter(h -> h != current && h.getTransferLimit() >= calc.getFinalAmount())
                .collect(Collectors.toList());
        if (approvers.isEmpty()) {
            ConsoleHelper.printError("결재 가능한 상위 결재권자가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        String[] options = approvers.stream()
                .map(h -> h.getName() + " (" + h.getPosition() + ", 전결한도 " + h.getTransferLimit() + ")")
                .toArray(String[]::new);
        int idx = ConsoleHelper.readMenuChoice("결재권자를 선택하세요:", options);
        Employee approver = approvers.get(idx - 1);
        calc.selectApprover(approver);
        calc.submitForApproval();
        ClaimCalculationDAO.save(calc);
        ConsoleHelper.printSuccess("결재 상신 완료. (시연 단계에서는 자동 승인 처리)");

        // 시연 단순화: 결재 후 즉시 승인
        if (ConsoleHelper.readYesNo("  결재권자가 승인했다고 가정하고 지급 이관을 진행할까요?")) {
            ClaimPayment payment = calc.approve();
            ClaimCalculationDAO.save(calc);
            if (payment != null) {
                ClaimPaymentDAO.save(payment);
                ConsoleHelper.printSuccess("지급 승인 및 이관 완료, 지급번호: " + payment.getPaymentNo());
            }
        }
        ConsoleHelper.waitEnter();
    }

    private static void showCalculation(ClaimCalculation calc) {
        ConsoleHelper.printDivider();
        System.out.println("  산출번호: " + calc.getCalculationNo());
        System.out.println("  대상 조사: " + calc.getInvestigation().getInvestigationNo());
        System.out.println("  총 인정 손해액: " + calc.getRecognizedDamage() + "원");
        System.out.println("  적용 과실 비율: " + calc.getFaultRatio() + "%");
        System.out.println("  자기부담금: " + calc.getDeductible() + "원");
        System.out.println("  최대 보장 한도: " + calc.getCoverageLimit() + "원");
        System.out.println("  최종 산출액: " + calc.getFinalAmount() + "원");
        System.out.println("  상태: " + calc.getStatus());
        ConsoleHelper.printDivider();
    }

    private static ClaimsHandler selectHandler() {
        List<ClaimsHandler> handlers = ClaimsHandlerDAO.findAll();
        if (handlers.isEmpty()) {
            ConsoleHelper.printError("등록된 보상담당자가 없습니다.");
            return null;
        }
        String[] options = handlers.stream()
                .map(h -> h.getName() + " (" + h.getPosition() + ", 전결한도 " + h.getTransferLimit() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 보상담당자를 선택하세요:", options);
        return handlers.get(choice - 1);
    }

    private static ClaimCalculation selectCalculation() {
        List<ClaimCalculation> available = ClaimCalculationDAO.findAll().stream()
                .filter(c -> c.getStatus() == CalculationStatus.CALCULATED
                        || c.getStatus() == CalculationStatus.APPROVAL_PENDING)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            ConsoleHelper.printError("처리할 산출 건이 없습니다. 먼저 손해 조사를 완료해주세요.");
            ConsoleHelper.waitEnter();
            return null;
        }
        String[] options = available.stream()
                .map(c -> c.getCalculationNo() + " - 산출액 " + c.getFinalAmount()
                        + "원 [" + c.getStatus() + "]")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 처리할 산출 건을 선택하세요:", options);
        return available.get(choice - 1);
    }
}
