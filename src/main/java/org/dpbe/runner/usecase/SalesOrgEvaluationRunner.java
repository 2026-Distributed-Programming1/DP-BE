package org.dpbe.runner.usecase;

import org.dpbe.dao.SalesOrgEvaluationDAO;
import org.dpbe.enums.ChannelType;
import org.dpbe.enums.EvaluationGrade;
import org.dpbe.runner.ConsoleHelper;
import org.dpbe.sales.SalesOrgEvaluation;

import java.time.LocalDate;
import java.util.List;

/**
 * UC: 영업조직을 평가한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 영업 관리자는 [영업 조직 평가] 항목을 클릭한다.
 *   2. 시스템은 채널별 성과 현황 테이블을 출력한다.
 *   3. 영업 관리자는 조회 조건(평가 기간, 채널 유형)을 입력하고 [조회] 버튼을 클릭한다.
 *   4. 시스템은 조회 조건에 맞는 성과 현황을 출력한다. (A4)
 *      (목표달성률 낮은 순 정렬)
 *   5. 영업 관리자는 테이블에서 평가할 채널 항목을 클릭한다.
 *   6. 시스템은 성과 상세 정보 패널을 출력한다.
 *   7. 영업 관리자는 [평가 등록] 버튼을 클릭한다. (A1)
 *   8. 시스템은 평가 등록 폼을 출력한다.
 *   9. 영업 관리자는 평가 등급 및 평가 의견을 입력하고 [저장] 버튼을 클릭한다. (A2, A3)(E1)
 *  10. 시스템은 평가 등록 완료 결과(평가번호, 등록일시, 채널명, 평가등급)를 출력한다.
 *  11. 영업 관리자는 [확인] 버튼을 클릭한다.
 *  12. 시스템은 평가 등록 완료 화면을 닫고 채널별 성과 현황 테이블 화면으로 돌아간다.
 *
 * Alternative:
 *   A1) [닫기] 버튼을 클릭한 경우
 *       → 성과 상세 정보 패널을 닫고 채널별 성과 현황 테이블 화면으로 돌아간다.
 *   A2) [취소] 버튼을 클릭한 경우
 *       → "작성 중인 내용이 저장되지 않습니다. 취소하시겠습니까?" 팝업 출력
 *       → 평가 등록 폼을 닫고 성과 상세 정보 패널로 돌아간다.
 *   A3) 평가 등급이 S 또는 A인 경우
 *       → [성과급 지급을 요청한다] 유스케이스로 이동 후 복귀
 *       → Basic Path 9번으로 이동한다.
 *   A4) 조회 조건에 해당하는 데이터가 없는 경우
 *       → "조회 가능한 데이터가 없습니다." 메시지 출력
 *
 * Exception:
 *   E1) 필수 항목(평가 등급)이 누락된 경우
 *       → "필수 항목을 입력해주세요." 메시지 출력 후 재입력 유도
 */
public class SalesOrgEvaluationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 영업조직을 평가한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 영업 관리자는 [영업 조직 평가] 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[영업] 메뉴 > [영업 조직 평가] 항목을 클릭합니다.");

        SalesOrgEvaluation evaluation = new SalesOrgEvaluation();

        // 2. 시스템은 채널별 성과 현황 테이블을 출력한다.
        evaluation.loadPerformanceTable();
        ConsoleHelper.printStage("시스템", "채널별 성과 현황 테이블을 출력합니다.");
        List<SalesOrgEvaluation> evaluationList = SalesOrgEvaluationDAO.findAll();
        if (evaluationList.isEmpty()) {
            ConsoleHelper.printInfo("  (저장된 평가 데이터가 없습니다.)");
        } else {
            ConsoleHelper.printInfo("  번호 | 채널명 | 평가등급");
            for (int i = 0; i < evaluationList.size(); i++) {
                SalesOrgEvaluation e = evaluationList.get(i);
                String grade = e.getEvaluationGrade() != null ? e.getEvaluationGrade().name() : "-";
                ConsoleHelper.printInfo("  " + (i + 1) + " | " + e.getChannelName() + " | " + grade);
            }
        }

        // 3. 영업 관리자는 조회 조건(평가 기간, 채널 유형)을 입력하고 [조회] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "조회 조건을 입력합니다.");
        LocalDate filterStart = ConsoleHelper.readDate("  평가 기간 시작일");
        LocalDate filterEnd = ConsoleHelper.readDate("  평가 기간 종료일");
        evaluation.setFilterStartDate(filterStart);
        evaluation.setFilterEndDate(filterEnd);

        int typeChoice = ConsoleHelper.readMenuChoice(
                "  채널 유형을 선택하세요.",
                "설계사", "대리점");
        evaluation.setChannelType(typeChoice == 1 ? ChannelType.DESIGNER : ChannelType.AGENCY);

        // 4. 시스템은 조회 결과를 출력한다. (A4)
        evaluation.search();
        ConsoleHelper.printStage("시스템", "조회 조건에 맞는 성과 현황을 출력합니다.");

        boolean hasData = ConsoleHelper.readYesNo("  조회된 데이터가 있습니까?");
        if (!hasData) {
            // A4) 조회 조건에 해당하는 데이터가 없는 경우
            evaluation.showNoResultMessage();
            ConsoleHelper.printStage("시스템", "조회 가능한 데이터가 없습니다.");
            ConsoleHelper.printInfo("[A4] 조회 조건을 수정한 후 [조회] 버튼을 클릭할 수 있습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 채널 성과 데이터 입력 (조회 결과 시뮬레이션)
        String channelName = ConsoleHelper.readNonEmpty("  채널명: ");
        evaluation.setChannelName(channelName);
        long salesResult = ConsoleHelper.readLong("  매출실적 (원): ");
        evaluation.setSalesResult(salesResult);
        int contractCount = ConsoleHelper.readInt("  계약건수: ");
        evaluation.setContractCount(contractCount);
        double achievementRate = ConsoleHelper.readDouble("  목표달성률 (%): ");
        evaluation.setAchievementRate(achievementRate);

        // 목표달성률 낮은 순 정렬 출력
        evaluation.sortByAchievementRate();
        ConsoleHelper.printStage("시스템", "목표달성률 낮은 순으로 정렬합니다.");
        ConsoleHelper.printInfo(channelName
                + " | 매출: " + salesResult + "원"
                + " | 계약: " + contractCount + "건"
                + " | 목표달성률: " + achievementRate + "%");

        // 5. 영업 관리자는 테이블에서 평가할 채널 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "평가할 채널 항목을 클릭합니다.");

        // 6. 시스템은 성과 상세 정보 패널을 출력한다.
        evaluation.openDetailPanel();
        ConsoleHelper.printStage("시스템", "성과 상세 정보 패널을 출력합니다.");
        ConsoleHelper.printInfo("채널명: " + channelName
                + " | 매출: " + salesResult + "원"
                + " | 계약: " + contractCount + "건"
                + " | 목표달성률: " + achievementRate + "%");

        // 7. 영업 관리자는 [평가 등록] 또는 [닫기]를 클릭한다. (A1)
        int panelAction = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "평가 등록", "닫기");

        if (panelAction == 2) {
            // A1) [닫기] 버튼을 클릭한 경우
            evaluation.closeDetailPanel();
            ConsoleHelper.printStage("시스템", "성과 상세 정보 패널을 닫고 채널별 성과 현황 테이블 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 8. 시스템은 평가 등록 폼을 출력한다.
        evaluation.openEvaluationForm();
        ConsoleHelper.printStage("시스템", "평가 등록 폼을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 평가등급(필수 - S/A/B/C/D) / 평가의견(선택)");

        // 9. 영업 관리자는 평가 등급 및 평가 의견을 입력하고 [저장] 또는 [취소]를 클릭한다. (A2, A3, E1)
        ConsoleHelper.printStage("영업관리자", "평가 정보를 입력합니다.");

        // 평가등급 선택 (필수)
        int gradeChoice = ConsoleHelper.readMenuChoice(
                "  평가등급을 선택하세요. (필수)",
                "S등급", "A등급", "B등급", "C등급", "D등급");
        EvaluationGrade grade;
        switch (gradeChoice) {
            case 1: grade = EvaluationGrade.S; break;
            case 2: grade = EvaluationGrade.A; break;
            case 3: grade = EvaluationGrade.B; break;
            case 4: grade = EvaluationGrade.C; break;
            default: grade = EvaluationGrade.D; break;
        }
        evaluation.setEvaluationGrade(grade);

        // 평가의견 입력 (선택)
        String comment = ConsoleHelper.readLine("  평가 의견 (없으면 엔터): ");
        if (!comment.isEmpty()) {
            evaluation.setEvaluationComment(comment);
        }

        int saveAction = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "저장", "취소");

        if (saveAction == 2) {
            // A2) [취소] 버튼을 클릭한 경우
            ConsoleHelper.printStage("시스템", "작성 중인 내용이 저장되지 않습니다. 취소하시겠습니까?");
            boolean confirmCancel = ConsoleHelper.readYesNo("  확인");
            if (confirmCancel) {
                // 시스템은 평가 등록 폼을 닫고 성과 상세 정보 패널로 돌아간다.
                evaluation.cancelEvaluation();
                ConsoleHelper.printInfo("[A2] 평가 등록 폼을 닫고 성과 상세 정보 패널로 돌아갑니다.");
            }
            ConsoleHelper.waitEnter();
            return;
        }

        // E1) 필수 항목(평가 등급) 누락 검증
        ConsoleHelper.printStage("시스템", "필수 항목 누락 여부를 검증합니다.");
        if (!evaluation.validateRequired()) {
            // E1) 필수 항목(평가 등급)이 누락된 경우
            evaluation.highlightError();
            ConsoleHelper.printError("[E1] 필수 항목을 입력해주세요. (평가등급)");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("필수 항목 검증 완료.");

        // A3) 평가 등급이 S 또는 A인 경우 → [성과급 지급을 요청한다]로 이동
        if (grade == EvaluationGrade.S || grade == EvaluationGrade.A) {
            ConsoleHelper.printStage("시스템", "평가 등급 " + grade + " — 성과급 지급 요건을 충족합니다.");
            int bonusAction = ConsoleHelper.readMenuChoice(
                    "[영업관리자] 처리를 선택하세요.",
                    "성과급 지급하기", "저장만 진행");
            if (bonusAction == 1) {
                // A3) [성과급 지급을 요청한다] 유스케이스로 이동
                evaluation.navigateToBonus();
                ConsoleHelper.printInfo("[A3] [성과급 지급을 요청한다] 유스케이스로 이동합니다.");
                BonusRequestRunner.run(evaluation);
                // A3) [성과급 지급을 요청한다] 유스케이스로부터 돌아온다. → Basic Path 9번으로 이동
                ConsoleHelper.printInfo("[A3] [성과급 지급을 요청한다]로부터 복귀합니다. 저장을 계속 진행합니다.");
            }
        }

        // 저장 처리
        evaluation.saveEvaluation();
        SalesOrgEvaluationDAO.save(evaluation);

        // 10. 시스템은 평가 등록 완료 결과를 출력한다.
        evaluation.showEvaluationResult();
        ConsoleHelper.printStage("시스템", "평가 등록 완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("평가번호: " + evaluation.getEvaluationNo()
                + " | 등록일시: " + evaluation.getEvaluatedAt()
                + " | 채널명: " + evaluation.getChannelName()
                + " | 평가등급: " + evaluation.getEvaluationGrade());

        // 11. 영업 관리자는 [확인] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[확인] 버튼을 클릭합니다.");
        ConsoleHelper.readLine("  [확인] (엔터를 눌러 확인): ");

        // 12. 시스템은 평가 등록 완료 화면을 닫고 채널별 성과 현황 테이블 화면으로 돌아간다.
        ConsoleHelper.printStage("시스템", "평가 등록 완료 화면을 닫고 채널별 성과 현황 테이블 화면으로 돌아갑니다.");

        ConsoleHelper.waitEnter();
    }
}