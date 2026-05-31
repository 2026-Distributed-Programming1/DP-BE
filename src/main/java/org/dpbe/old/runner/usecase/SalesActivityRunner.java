package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.SalesManager;
import org.dpbe.old.dao.SalesActivityManagementDAO;
import org.dpbe.old.dao.SalesManagerDAO;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.domain.sales.entity.SalesActivityManagement;

import java.time.LocalDate;
import java.util.List;

/**
 * UC: 영업 활동을 관리한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 영업 관리자는 [영업 활동 관리] 항목을 클릭한다.
 *   2. 시스템은 채널별 영업활동 현황 테이블을 출력한다.
 *   3. 영업 관리자는 조회 조건(관리 기간, 채널 유형)을 입력하고 [조회] 버튼을 클릭한다.
 *   4. 시스템은 조회 조건에 맞는 영업활동 현황을 출력한다. (A4)
 *      (목표달성률 낮은 순 정렬 / 70% 미만 셀 강조)
 *   5. 영업 관리자는 테이블에서 관리할 채널 항목을 클릭한다.
 *   6. 시스템은 채널 상세 활동 내역 패널을 출력한다.
 *   7. 영업 관리자는 [개선 지시 등록] 버튼을 클릭한다. (A1, A2)
 *   8. 시스템은 개선 지시 입력 폼을 출력한다.
 *   9. 영업 관리자는 개선 지시 내용 및 수정 목표를 입력하고 [저장] 버튼을 클릭한다. (A3)
 *  10. 시스템은 "개선 지시가 등록되었습니다."라는 팝업 메시지와 함께
 *      등록 완료 결과(관리번호, 등록일시, 담당채널)를 출력한다. (E1)
 *
 * Alternative:
 *   A1) 인원 충당이 필요한 경우
 *       → [판매채널을 모집한다] 유스케이스로 이동한다.
 *   A2) [닫기] 버튼을 클릭한 경우
 *       → 상세 활동 내역 패널을 닫고 현황 테이블 화면으로 돌아간다.
 *   A3) [취소] 버튼을 클릭한 경우
 *       → "작성 중인 내용이 저장되지 않습니다. 취소하시겠습니까?" 팝업 출력
 *       → 개선 지시 입력 폼을 닫고 상세 활동 내역 패널로 돌아간다.
 *   A4) 조회 조건에 해당하는 영업활동 데이터가 없는 경우
 *       → "조회 가능한 영업활동 데이터가 없습니다." 메시지 출력
 *
 * Exception:
 *   E1) 저장 실패 시
 *       → "저장에 실패했습니다. 다시 시도해 주세요." 팝업 메시지 출력
 */
public class SalesActivityRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 영업 활동을 관리한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 영업 관리자는 [영업 활동 관리] 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[영업] 메뉴 > [영업 활동 관리] 항목을 클릭합니다.");

        List<SalesManager> managers = SalesManagerDAO.findAll();
        if (managers.isEmpty()) {
            ConsoleHelper.printError("등록된 영업 관리자가 없습니다. 먼저 영업 관리자를 등록해주세요.");
            ConsoleHelper.waitEnter();
            return;
        }
        SalesManager manager = managers.get(0);
        SalesActivityManagement activity = new SalesActivityManagement();
        activity.setManagerName(manager.getName());

        // 2. 시스템은 채널별 영업활동 현황 테이블을 출력한다.
        activity.loadActivityTable();
        ConsoleHelper.printStage("시스템", "채널별 영업활동 현황 테이블을 출력합니다.");
        List<SalesActivityManagement> activityList = SalesActivityManagementDAO.findAll();
        if (activityList.isEmpty()) {
            ConsoleHelper.printInfo("  (저장된 영업활동 데이터가 없습니다.)");
        } else {
            ConsoleHelper.printInfo("  번호 | 채널명 | 방문건수 | 계약건수 | 전환율 | 목표달성률");
            for (int i = 0; i < activityList.size(); i++) {
                SalesActivityManagement a = activityList.get(i);
                ConsoleHelper.printInfo("  " + (i + 1)
                        + " | " + a.getChannelName()
                        + " | " + (a.getVisitCount() != null ? a.getVisitCount() : "-")
                        + " | " + (a.getContractCount() != null ? a.getContractCount() : "-")
                        + " | " + (a.getConversionRate() != null ? String.format("%.1f%%", a.getConversionRate()) : "-")
                        + " | " + (a.getAchievementRate() != null ? String.format("%.1f%%", a.getAchievementRate()) : "-"));
            }
        }

        // 3. 영업 관리자는 조회 조건(관리 기간, 채널 유형)을 입력하고 [조회] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "조회 조건을 입력합니다.");
        LocalDate startDate = ConsoleHelper.readDate("  관리 기간 시작일");
        LocalDate endDate = ConsoleHelper.readDate("  관리 기간 종료일");
        activity.setStartDate(startDate);
        activity.setEndDate(endDate);

        int typeChoice = ConsoleHelper.readMenuChoice(
                "  채널 유형을 선택하세요.",
                "설계사", "대리점");
        activity.setChannelType(typeChoice == 1 ? ChannelType.DESIGNER : ChannelType.AGENCY);

        // 4. 시스템은 조회 결과를 출력한다. (A4)
        activity.search();
        ConsoleHelper.printStage("시스템", "조회 조건에 맞는 영업활동 현황을 출력합니다.");

        // A4) 조회 결과 없는 경우 시뮬레이션 선택
        boolean hasData = ConsoleHelper.readYesNo("  조회된 데이터가 있습니까?");
        if (!hasData) {
            // A4) 조회 조건에 해당하는 영업활동 데이터가 없는 경우
            activity.showNoResultMessage();
            ConsoleHelper.printStage("시스템", "조회 가능한 영업활동 데이터가 없습니다.");
            ConsoleHelper.printInfo("[A4] 조회 조건을 수정한 후 다시 조회할 수 있습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 채널 활동 데이터 입력 (조회 결과 시뮬레이션)
        String channelName = ConsoleHelper.readNonEmpty("  채널명: ");
        activity.setChannelName(channelName);
        int visitCount = ConsoleHelper.readPositiveInt("  방문건수: ");
        activity.setVisitCount(visitCount);
        int contractCount = ConsoleHelper.readInt("  계약건수: ");
        activity.setContractCount(contractCount);
        double achievementRate = ConsoleHelper.readDouble("  목표달성률 (%): ");
        activity.setAchievementRate(achievementRate);

        // 목표달성률 낮은 순 정렬 및 70% 미만 강조 출력
        activity.sortByAchievementRate();
        ConsoleHelper.printStage("시스템", "목표달성률 낮은 순으로 정렬합니다.");
        String prefix = achievementRate < 70.0 ? "⚠️  [강조] " : "";
        if (achievementRate < 70.0) activity.highlightLowAchievement();
        ConsoleHelper.printInfo(prefix + channelName
                + " | 방문: " + visitCount + "건"
                + " | 계약: " + contractCount + "건"
                + " | 전환율: " + String.format("%.1f", activity.getConversionRate()) + "%"
                + " | 목표달성률: " + achievementRate + "%");

        // 5. 영업 관리자는 테이블에서 관리할 채널 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "관리할 채널 항목을 클릭합니다.");

        // 6. 시스템은 채널 상세 활동 내역 패널을 출력한다.
        activity.openDetailPanel();
        ConsoleHelper.printStage("시스템", "채널 상세 활동 내역 패널을 출력합니다.");
        ConsoleHelper.printInfo("채널명: " + channelName
                + " | 방문: " + visitCount + "건"
                + " | 계약: " + contractCount + "건"
                + " | 전환율: " + String.format("%.1f", activity.getConversionRate()) + "%"
                + " | 목표달성률: " + achievementRate + "%");

        // 7. 영업 관리자는 [개선 지시 등록], [채널 모집], [닫기] 중 선택한다. (A1, A2)
        int panelAction = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "개선 지시 등록", "채널 모집 (인원 충당 필요)", "닫기");

        if (panelAction == 2) {
            // A1) 인원 충당이 필요한 경우 → [판매채널을 모집한다]로 이동
            activity.navigateToRecruitment();
            ConsoleHelper.printInfo("[A1] [판매채널을 모집한다] 유스케이스로 이동합니다.");
            ChannelRecruitmentRunner.run();
            return;
        }

        if (panelAction == 3) {
            // A2) [닫기] 버튼을 클릭한 경우
            activity.closeDetailPanel();
            ConsoleHelper.printStage("시스템", "상세 활동 내역 패널을 닫고 채널별 영업활동 현황 테이블 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 8. 시스템은 개선 지시 입력 폼을 출력한다.
        activity.openImprovementForm();
        ConsoleHelper.printStage("시스템", "개선 지시 입력 폼을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 개선 지시 내용 / 수정 목표");

        // 9. 영업 관리자는 개선 지시 내용 및 수정 목표를 입력하고 [저장] 또는 [취소]를 클릭한다. (A3)
        ConsoleHelper.printStage("영업관리자", "개선 지시 내용을 입력합니다.");
        String improvementContent = ConsoleHelper.readNonEmpty("  개선 지시 내용: ");
        int revisedTarget = ConsoleHelper.readPositiveInt("  수정 목표 (건): ");
        activity.setImprovementContent(improvementContent);
        activity.setRevisedTarget(revisedTarget);

        int saveAction = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "저장", "취소");

        if (saveAction == 2) {
            // A3) [취소] 버튼을 클릭한 경우
            ConsoleHelper.printStage("시스템", "작성 중인 내용이 저장되지 않습니다. 취소하시겠습니까?");
            boolean confirmCancel = ConsoleHelper.readYesNo("  확인");
            if (confirmCancel) {
                // 시스템은 개선 지시 입력 폼을 닫고 상세 활동 내역 패널로 돌아간다.
                activity.cancelImprovement();
                ConsoleHelper.printInfo("[A3] 개선 지시 입력 폼을 닫고 상세 활동 내역 패널로 돌아갑니다.");
            }
            ConsoleHelper.waitEnter();
            return;
        }

        // 저장 처리
        activity.saveImprovement();

        // E1) 저장 실패 시 (시뮬레이션: 항상 성공으로 처리)
        boolean saveSuccess = true;
        if (!saveSuccess) {
            activity.showSaveError();
            ConsoleHelper.printStage("시스템", "저장에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 10. 시스템은 "개선 지시가 등록되었습니다." 팝업과 등록 완료 결과를 출력한다. (E1)
        SalesActivityManagementDAO.save(activity);
        activity.showSaveSuccess();
        ConsoleHelper.printStage("시스템", "개선 지시가 등록되었습니다.");
        ConsoleHelper.printStage("시스템", "등록 완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("관리번호: " + activity.getActivityNo()
                + " | 등록일시: " + activity.getRegisteredAt()
                + " | 담당채널: " + activity.getChannelName());

        ConsoleHelper.waitEnter();
    }
}