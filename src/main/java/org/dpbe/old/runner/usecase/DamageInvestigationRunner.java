package org.dpbe.old.runner.usecase;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.old.dao.ClaimCalculationDAO;
import org.dpbe.old.dao.ClaimRequestDAO;
import org.dpbe.old.dao.ClaimsHandlerDAO;
import org.dpbe.old.dao.DamageInvestigationDAO;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.domain.common.enums.InvestigationStatus;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC4: 「손해 조사를 한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 청구 건 배정 → 과실 비율 입력 → 의견 작성 → 결과 선택 → 완료
 * 분기:
 * - A1: 보완 서류 요청
 * - A2: 추가 조사 지시
 * - A3: 면책 종결
 * - E1: 과실 비율 합 != 100% → 재입력
 * - E2: 필수 입력 누락 시 완료 불가
 */
public class DamageInvestigationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC4: 손해 조사를 한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 보상담당자 선택
        ClaimsHandler handler = selectHandler();
        if (handler == null) return;

        // 2) 조사할 청구 건 또는 진행 중 조사 선택
        DamageInvestigation investigation = selectOrCreateInvestigation(handler);
        if (investigation == null) return;

        // 3) 메인 처리 루프
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[보상담당자] 조사 작업을 선택하세요:",
                    "과실 비율 및 손해액 입력 / 의견 작성 / 결과 선택 → 완료",
                    "보완 서류 요청 (A1)",
                    "추가 조사 지시 (A2)",
                    "면책 종결 처리 (A3)",
                    "현재 조사 정보 보기",
                    "메인 메뉴로 돌아가기");
            switch (choice) {
                case 1:
                    if (mainFlow(investigation, handler)) return;
                    break;
                case 2:
                    requestSupplement(investigation);
                    DamageInvestigationDAO.save(investigation);
                    break;
                case 3:
                    requestAdditional(investigation);
                    DamageInvestigationDAO.save(investigation);
                    break;
                case 4:
                    closeAsRejected(investigation);
                    return;
                case 5:
                    showInvestigation(investigation);
                    break;
                case 6:
                    return;
            }
        }
    }

    /** 정상 흐름: 입력 → 검증 → 완료 → UC5 이관 */
    private static boolean mainFlow(DamageInvestigation inv, ClaimsHandler handler) {
        // 손해액
        long damage = ConsoleHelper.readLong("[보상담당자] 총 인정 손해액(원): ");
        inv.enterRecognizedDamage(damage);

        // 과실 비율 (E1: 합 != 100%)
        while (true) {
            double our = ConsoleHelper.readDouble("  우리 고객 과실 비율(%): ");
            double counter = ConsoleHelper.readDouble("  상대방 과실 비율(%): ");
            inv.enterFaultRatio(our, counter);
            if (inv.validateFaultRatio()) {
                ConsoleHelper.printSuccess("과실 비율 검증 통과 (합 100%)");
                break;
            }
            ConsoleHelper.printError("[E1] 과실 비율의 합이 100%가 아닙니다. 다시 입력해주세요.");
        }

        // 조사 의견
        String opinion = ConsoleHelper.readNonEmpty("[보상담당자] 조사 의견 및 합의 내용: ");
        inv.enterOpinion(opinion);

        // 처리 결과 선택
        int resultChoice = ConsoleHelper.readMenuChoice("처리 결과를 선택하세요:",
                "지급 승인", "면책");
        if (resultChoice == 1) {
            inv.selectResult(InvestigationResult.APPROVED);
        } else {
            inv.selectResult(InvestigationResult.REJECTED);
            String reason = ConsoleHelper.readNonEmpty("  면책 사유: ");
            inv.enterRejectReason(reason);
        }

        // 검증 (E2)
        if (!inv.validateRequired()) {
            ConsoleHelper.printError("[E2] 필수 입력값이 누락되어 완료할 수 없습니다.");
            return false;
        }

        // 완료 (지급 승인 시 UC5 → UC6 체인 이관)
        if (inv.getResult() == InvestigationResult.APPROVED) {
            ClaimCalculation calc = inv.complete();
            DamageInvestigationDAO.save(inv);
            if (calc != null) {
                ClaimCalculationDAO.save(calc);
                ConsoleHelper.printSuccess("조사 완료, 보험금 산출로 이관: " + calc.getCalculationNo());
                ConsoleHelper.printInfo("→ 보험금 산출 단계로 이관합니다.");
                ClaimCalculationRunner.run(handler, calc);
            }
        } else {
            inv.closeAsRejected();
            DamageInvestigationDAO.save(inv);
            ConsoleHelper.printInfo("면책으로 종결되었습니다.");
            ConsoleHelper.waitEnter();
        }
        return true;
    }

    /** A1: 보완 서류 요청 */
    private static void requestSupplement(DamageInvestigation inv) {
        ConsoleHelper.printStage("보상담당자", "[A1] 보완 서류를 요청합니다.");
        String items = ConsoleHelper.readNonEmpty("  요청 서류 (쉼표 구분, 예: 진단서,사고경위서): ");
        String message = ConsoleHelper.readNonEmpty("  안내 메시지: ");
        List<String> itemList = Arrays.stream(items.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        inv.requestSupplement(itemList, message);
        ConsoleHelper.printSuccess("보완 서류 요청 발송 완료");
    }

    /** A2: 추가 조사 지시 */
    private static void requestAdditional(DamageInvestigation inv) {
        ConsoleHelper.printStage("보상담당자", "[A2] 추가 조사를 지시합니다.");
        int locChoice = ConsoleHelper.readMenuChoice("방문지를 선택하세요:",
                "현장", "정비소", "병원");
        String[] locations = {"현장", "정비소", "병원"};
        LocalDateTime schedule = ConsoleHelper.readDateTime("  조사 일정");
        String reason = ConsoleHelper.readNonEmpty("  추가 조사 사유: ");
        inv.requestAdditionalInvestigation(locations[locChoice - 1], schedule, reason);
        ConsoleHelper.printSuccess("추가 조사 지시 등록 완료");
    }

    /** A3: 면책 종결 */
    private static void closeAsRejected(DamageInvestigation inv) {
        ConsoleHelper.printStage("보상담당자", "[A3] 면책으로 종결 처리합니다.");
        String reason = ConsoleHelper.readNonEmpty("  면책 사유: ");
        inv.selectResult(InvestigationResult.REJECTED);
        inv.enterRejectReason(reason);
        inv.closeAsRejected();
        DamageInvestigationDAO.save(inv);
        ConsoleHelper.printInfo("면책 종결되었습니다.");
        ConsoleHelper.waitEnter();
    }

    private static void showInvestigation(DamageInvestigation inv) {
        ConsoleHelper.printDivider();
        System.out.println("  조사번호: " + inv.getInvestigationNo());
        System.out.println("  대상 청구: " + inv.getClaim().getClaimNo());
        System.out.println("  담당자: " + (inv.getHandler() != null ? inv.getHandler().getName() : "(미배정)"));
        System.out.println("  상태: " + inv.getStatus());
        System.out.println("  손해액: " + inv.getRecognizedDamage());
        System.out.println("  과실비율: 우리 " + inv.getOurFaultRatio() + "% / 상대 " + inv.getCounterFaultRatio() + "%");
        if (inv.getOpinion() != null) System.out.println("  의견: " + inv.getOpinion());
        if (inv.getResult() != null) System.out.println("  결과: " + inv.getResult());
        if (inv.getSupplementRequest() != null)
            System.out.println("  보완요청: " + inv.getSupplementRequest().getRequestedItems());
        if (inv.getAdditionalInvestigation() != null)
            System.out.println("  추가조사: " + inv.getAdditionalInvestigation().getVisitLocation()
                    + ", " + inv.getAdditionalInvestigation().getSchedule());
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

    /** 새 조사를 시작하거나 진행 중 조사를 이어서 처리 */
    private static DamageInvestigation selectOrCreateInvestigation(ClaimsHandler handler) {
        // 진행 중 조사
        List<DamageInvestigation> allInvestigations = DamageInvestigationDAO.findAll();
        List<DamageInvestigation> ongoing = allInvestigations.stream()
                .filter(i -> i.getStatus() == InvestigationStatus.NEW_ASSIGNED
                        || i.getStatus() == InvestigationStatus.INVESTIGATING)
                .collect(Collectors.toList());

        // 조사가 시작되지 않은 청구 건
        List<ClaimRequest> pendingClaims = ClaimRequestDAO.findAll().stream()
                .filter(c -> allInvestigations.stream().noneMatch(
                        i -> i.getClaim().getClaimNo().equals(c.getClaimNo())))
                .collect(Collectors.toList());

        if (ongoing.isEmpty() && pendingClaims.isEmpty()) {
            ConsoleHelper.printError("처리할 조사 건이 없습니다. 먼저 보험금을 청구해주세요.");
            ConsoleHelper.waitEnter();
            return null;
        }

        // 메뉴 구성
        List<String> options = new java.util.ArrayList<>();
        for (DamageInvestigation i : ongoing) {
            options.add("[진행중] " + i.getInvestigationNo() + " (" + i.getClaim().getClaimNo() + ")");
        }
        for (ClaimRequest c : pendingClaims) {
            options.add("[신규배정] " + c.getClaimNo() + " (" + c.getCustomer().getName() + ")");
        }
        int choice = ConsoleHelper.readMenuChoice("[시스템] 처리할 조사 건을 선택하세요:",
                options.toArray(new String[0]));

        if (choice <= ongoing.size()) {
            return ongoing.get(choice - 1);
        } else {
            ClaimRequest claim = pendingClaims.get(choice - 1 - ongoing.size());
            DamageInvestigation inv = new DamageInvestigation(claim);
            inv.assignHandler(handler);
            DamageInvestigationDAO.save(inv);
            ConsoleHelper.printSuccess("새 조사 건이 생성되어 " + handler.getName() + "에게 배정되었습니다.");
            return inv;
        }
    }
}
