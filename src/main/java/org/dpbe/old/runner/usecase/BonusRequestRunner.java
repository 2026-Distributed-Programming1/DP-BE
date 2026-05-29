package org.dpbe.old.runner.usecase;

import org.dpbe.old.dao.BonusRequestDAO;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.domain.sales.entity.BonusRequest;
import org.dpbe.domain.sales.entity.SalesOrgEvaluation;

/**
 * UC: 성과급 지급을 요청한다 시나리오 진행자
 *
 * Basic Path:
 *   1. [영업조직을 평가한다] A3) 2번으로부터 넘어온다.
 *   2. 시스템은 성과급 지급 요청 화면을 출력한다. (채널명, 평가등급, 기본급, 성과급 금액 자동 산출)
 *   3. 영업 관리자는 성과급 금액을 확인하고 [요청 제출] 버튼을 클릭한다. (A1)
 *   4. 시스템은 "성과급 지급을 요청하시겠습니까?"라는 확인 팝업을 출력한다.
 *   5. 영업 관리자는 [확인] 버튼을 클릭한다.
 *   6. 시스템은 성과급 지급 요청서를 인사 담당자에게 자동 전달하고
 *      요청 완료 결과(요청번호, 요청일시, 대상채널, 요청금액)를 출력한다. (E1)
 *   7. 영업 관리자는 [확인] 버튼을 클릭한다.
 *   8. 시스템은 [영업조직을 평가한다] 유스케이스 A3) 3번으로 복귀한다.
 *
 * Alternative:
 *   A1) [취소] 버튼을 클릭한 경우
 *       → 성과급 지급 화면을 닫고 [영업조직을 평가한다]로 복귀한다.
 *
 * Exception:
 *   E1) 요청 처리 실패 시
 *       → "요청 처리에 실패했습니다. 다시 시도해 주세요." 팝업 출력
 *       → 영업 관리자는 [재시도] 버튼을 클릭한다.
 */
public class BonusRequestRunner {

    public static void run(SalesOrgEvaluation evaluation) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 성과급 지급을 요청한다");
        ConsoleHelper.printDoubleDivider();

        ConsoleHelper.printInfo("[영업조직을 평가한다] A3) 평가 등급 S/A로 성과급 지급 요청 화면으로 이동합니다.");

        BonusRequest bonusRequest = new BonusRequest();

        // 2. 평가 정보 자동 표시
        bonusRequest.loadRequestScreen();
        ConsoleHelper.printStage("시스템", "성과급 지급 요청 화면을 출력합니다.");
        String evalGradeStr = evaluation.getEvaluationGrade() != null ? evaluation.getEvaluationGrade().name() : "-";
        ConsoleHelper.printInfo("[평가 정보] 평가번호: " + evaluation.getEvaluationNo()
                + " | 채널명: " + evaluation.getChannelName()
                + " | 평가등급: " + evalGradeStr);

        bonusRequest.setChannelName(evaluation.getChannelName());
        bonusRequest.setChannelType(evaluation.getChannelType() != null
                ? evaluation.getChannelType() : ChannelType.DESIGNER);
        bonusRequest.setEvaluationNo(evaluation.getEvaluationNo());
        bonusRequest.setEvaluationGrade(evaluation.getEvaluationGrade() != null
                ? evaluation.getEvaluationGrade() : EvaluationGrade.S);

        ConsoleHelper.printStage("영업관리자", "성과급 지급 요청 추가 정보를 입력합니다.");
        long baseSalary = ConsoleHelper.readLong("  기본급 (원): ");
        bonusRequest.setBaseSalary(baseSalary);
        String requestReason = ConsoleHelper.readNonEmpty("  요청사유: ");
        bonusRequest.setRequestReason(requestReason);

        Double bonusAmount = bonusRequest.calculateBonus();
        ConsoleHelper.printStage("시스템", "성과급 금액을 자동 산출합니다.");
        ConsoleHelper.printInfo("평가등급: " + evalGradeStr
                + " | 지급비율: " + (int)(bonusRequest.getBonusRatio() * 100) + "%"
                + " | 기본급: " + baseSalary + "원"
                + " | 산출된 성과급: " + bonusAmount + "원");

        int action = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "요청 제출", "취소");

        if (action == 2) {
            bonusRequest.cancel();
            ConsoleHelper.printStage("시스템", "성과급 지급 화면을 닫습니다.");
            ConsoleHelper.printInfo("[A1] [영업조직을 평가한다] 유스케이스로 복귀합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        bonusRequest.showConfirmPopup();
        ConsoleHelper.printStage("시스템", "성과급 지급을 요청하시겠습니까?");
        boolean confirm = ConsoleHelper.readYesNo("  확인");
        if (!confirm) {
            ConsoleHelper.waitEnter();
            return;
        }

        bonusRequest.submit();
        BonusRequestDAO.save(bonusRequest);

        bonusRequest.showRequestResult();
        ConsoleHelper.printStage("시스템", "요청 완료 결과를 출력합니다.");
        String channelTypeStr = bonusRequest.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점";
        ConsoleHelper.printInfo("요청번호: " + bonusRequest.getRequestNo()
                + " | 요청일시: " + bonusRequest.getRequestedAt()
                + " | 대상채널: " + bonusRequest.getChannelName() + "(" + channelTypeStr + ")"
                + " | 요청금액: " + bonusRequest.getBonusAmount() + "원");

        ConsoleHelper.printStage("영업관리자", "[확인] 버튼을 클릭합니다.");
        ConsoleHelper.readLine("  [확인] (엔터를 눌러 확인): ");
        ConsoleHelper.waitEnter();
    }

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 성과급 지급을 요청한다");
        ConsoleHelper.printDoubleDivider();

        // 1. [영업조직을 평가한다] A3) 2번으로부터 넘어온다.
        ConsoleHelper.printInfo("[영업조직을 평가한다] A3) 평가 등급 S/A로 성과급 지급 요청 화면으로 이동합니다.");

        BonusRequest bonusRequest = new BonusRequest();

        // 2. 시스템은 성과급 지급 요청 화면을 출력한다.
        bonusRequest.loadRequestScreen();
        ConsoleHelper.printStage("시스템", "성과급 지급 요청 화면을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 채널명 / 채널유형 / 평가번호 / 평가등급(S/A) / 기본급 / 요청사유");

        // 채널명 입력
        ConsoleHelper.printStage("영업관리자", "성과급 지급 요청 정보를 입력합니다.");
        String channelName = ConsoleHelper.readNonEmpty("  채널명: ");
        bonusRequest.setChannelName(channelName);

        // 채널유형 선택
        int typeChoice = ConsoleHelper.readMenuChoice(
                "  채널유형을 선택하세요.",
                "설계사", "대리점");
        bonusRequest.setChannelType(typeChoice == 1 ? ChannelType.DESIGNER : ChannelType.AGENCY);

        // 평가번호 입력
        String evaluationNo = ConsoleHelper.readNonEmpty("  평가번호: ");
        bonusRequest.setEvaluationNo(evaluationNo);

        // 평가등급 선택 (S 또는 A만 성과급 지급 대상)
        int gradeChoice = ConsoleHelper.readMenuChoice(
                "  평가등급을 선택하세요. (S/A 등급만 성과급 지급 대상)",
                "S등급 (지급비율 150%)", "A등급 (지급비율 120%)");
        bonusRequest.setEvaluationGrade(gradeChoice == 1 ? EvaluationGrade.S : EvaluationGrade.A);

        // 기본급 입력
        long baseSalary = ConsoleHelper.readLong("  기본급 (원): ");
        bonusRequest.setBaseSalary(baseSalary);

        // 요청사유 입력
        String requestReason = ConsoleHelper.readNonEmpty("  요청사유: ");
        bonusRequest.setRequestReason(requestReason);

        // 성과급 금액 자동 산출
        Double bonusAmount = bonusRequest.calculateBonus();
        String gradeStr = bonusRequest.getEvaluationGrade() == EvaluationGrade.S ? "S" : "A";
        ConsoleHelper.printStage("시스템", "성과급 금액을 자동 산출합니다.");
        ConsoleHelper.printInfo("평가등급: " + gradeStr
                + " | 지급비율: " + (int)(bonusRequest.getBonusRatio() * 100) + "%"
                + " | 기본급: " + baseSalary + "원"
                + " | 산출된 성과급: " + bonusAmount + "원");

        // 3. 영업 관리자는 성과급 금액을 확인하고 [요청 제출] 또는 [취소]를 클릭한다. (A1)
        int action = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "요청 제출", "취소");

        if (action == 2) {
            // A1) [취소] 버튼을 클릭한 경우
            bonusRequest.cancel();
            ConsoleHelper.printStage("시스템", "성과급 지급 화면을 닫습니다.");
            bonusRequest.returnToEvaluation();
            ConsoleHelper.printInfo("[A1] [영업조직을 평가한다] 유스케이스로 복귀합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 4. 시스템은 "성과급 지급을 요청하시겠습니까?" 확인 팝업을 출력한다.
        bonusRequest.showConfirmPopup();
        ConsoleHelper.printStage("시스템", "성과급 지급을 요청하시겠습니까?");

        // 5. 영업 관리자는 [확인] 버튼을 클릭한다.
        boolean confirm = ConsoleHelper.readYesNo("  확인");
        if (!confirm) {
            ConsoleHelper.waitEnter();
            return;
        }

        // 6. 시스템은 성과급 지급 요청서를 인사 담당자에게 자동 전달하고 요청 완료 결과를 출력한다. (E1)
        bonusRequest.submit();

        // E1) 요청 처리 실패 시 재시도 (시뮬레이션: 항상 성공으로 처리)
        boolean submitSuccess = true;
        if (!submitSuccess) {
            bonusRequest.showRequestError();
            ConsoleHelper.printStage("시스템", "요청 처리에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.readMenuChoice("[영업관리자] 처리를 선택하세요.", "재시도");
            bonusRequest.retry();
            bonusRequest.submit();
        }

        BonusRequestDAO.save(bonusRequest);

        bonusRequest.showRequestResult();
        ConsoleHelper.printStage("시스템", "성과급 지급 요청서를 인사 담당자에게 자동 전달합니다.");
        ConsoleHelper.printStage("시스템", "요청 완료 결과를 출력합니다.");
        String channelTypeStr = bonusRequest.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점";
        ConsoleHelper.printInfo("요청번호: " + bonusRequest.getRequestNo()
                + " | 요청일시: " + bonusRequest.getRequestedAt()
                + " | 대상채널: " + bonusRequest.getChannelName() + "(" + channelTypeStr + ")"
                + " | 요청금액: " + bonusRequest.getBonusAmount() + "원");

        // 7. 영업 관리자는 [확인] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[확인] 버튼을 클릭합니다.");
        ConsoleHelper.readLine("  [확인] (엔터를 눌러 확인): ");

        // 8. 시스템은 [영업조직을 평가한다] A3) 3번으로 복귀한다.
        bonusRequest.returnToEvaluation();
        ConsoleHelper.printInfo("[영업조직을 평가한다] A3) 3번으로 복귀합니다.");

        ConsoleHelper.waitEnter();
    }
}