package org.dpbe.old.runner.usecase;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.EducationTrainer;
import org.dpbe.old.dao.EducationPlanDAO;
import org.dpbe.old.dao.EducationPreparationDAO;
import org.dpbe.old.dao.EducationTrainerDAO;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC: 교육 제반을 등록한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 영업교육 담당자는 [교육 제반 등록] 항목을 클릭한다.
 *   2. 시스템은 교육 제반 등록 화면을 출력한다.
 *   3. 영업교육 담당자는 조회 조건을 입력하고 [조회] 버튼을 클릭한다.
 *   4. 시스템은 승인된 교육 계획안 목록을 출력한다. (A3)
 *   5. 영업교육 담당자는 교육 계획안 항목을 클릭한다.
 *   6. 시스템은 제반 등록 화면을 출력한다.
 *   7. 영업교육 담당자는 제반 정보를 입력하고 [저장] 버튼을 클릭한다. (A1)
 *   8. 시스템은 필수 항목 누락 여부를 검증하고 저장 완료 팝업을 출력한다. (E1)
 *   9. 영업교육 담당자는 [확인] 버튼을 클릭한다.
 *  10. 시스템은 등록 완료 결과를 출력한다. (A2)
 *
 * Alternative:
 *   A1) [취소] 버튼을 클릭한 경우 → 목록 화면으로 복귀
 *   A2) 교육 진행이 필요한 경우 → 교육을 진행한다 유스케이스로 이동
 *   A3) 조회 조건에 해당하는 교육 계획안이 없는 경우 → 안내 메시지 출력
 *
 * Exception:
 *   E1) 필수 항목이 누락된 경우 → 오류 메시지 출력
 */
public class EducationPreparationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 교육 제반을 등록한다");
        ConsoleHelper.printDoubleDivider();

        // 2. 시스템은 교육 제반 등록 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "교육 제반 등록 화면을 출력합니다.");

        // 3~4. 승인된 교육 계획안 목록 조회
        List<EducationPlan> approvedPlans = EducationPlanDAO.findAll().stream()
                .filter(p -> p.getStatus().equals("승인"))
                .collect(Collectors.toList());

        // A3) 조회 조건에 해당하는 교육 계획안이 없는 경우
        if (approvedPlans.isEmpty()) {
            ConsoleHelper.printError("[A3] 조회 가능한 교육 계획안이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 5. 영업교육 담당자는 교육 계획안 항목을 클릭한다.
        String[] planOptions = approvedPlans.stream()
                .map(p -> "[" + p.getPlanNumber() + "] " + p.getEducationName()
                        + " | " + p.getChannelType()
                        + " | 대상자수: " + p.getTargetCount())
                .toArray(String[]::new);
        int planChoice = ConsoleHelper.readMenuChoice("[영업교육담당자] 교육 계획안을 선택하세요:", planOptions);
        EducationPlan selectedPlan = approvedPlans.get(planChoice - 1);

        List<EducationTrainer> trainerList = EducationTrainerDAO.findAll();
        if (trainerList.isEmpty()) {
            ConsoleHelper.printError("등록된 영업교육 담당자가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        EducationTrainer trainer = trainerList.get(0);
        EducationPreparation preparation = trainer.registerEducationPreparation();
        preparation.setPlanNo(String.valueOf(selectedPlan.getPlanNumber()));

        // 6. 시스템은 제반 등록 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "제반 정보 입력 항목: 교육장소 / 강사명 / 교재현황 / 대상자명단 / 기타사항");

        // 7. 영업교육 담당자는 제반 정보를 입력한다. (A1)
        int action = ConsoleHelper.readMenuChoice(
                "[영업교육담당자] 작업을 선택하세요.",
                "제반 정보 입력", "취소");

        if (action == 2) {
            // A1) [취소] 버튼을 클릭한 경우
            ConsoleHelper.printInfo("[A1] 교육 계획안 목록 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        ConsoleHelper.printStage("영업교육담당자", "제반 정보를 입력합니다.");
        String location = ConsoleHelper.readNonEmpty("  교육장소: ");
        String instructor = ConsoleHelper.readNonEmpty("  강사명: ");
        String textbook = ConsoleHelper.readNonEmpty("  교재 준비 현황: ");
        String notice = ConsoleHelper.readLine("  기타 준비 사항 (없으면 엔터): ");
        preparation.enterPreparationInfo(location, instructor, notice);
        preparation.setTextbookStatus(textbook);

        // 출석 대상자 명단 입력
        ConsoleHelper.printStage("영업교육담당자", "교육 대상자 명단을 입력합니다.");
        String attendeesInput = ConsoleHelper.readNonEmpty("  대상자 이름 (쉼표로 구분): ");
        for (String name : attendeesInput.split(",")) {
            preparation.addAttendee(name.trim());
        }

        // 8. 시스템은 필수 항목 누락 여부를 검증한다. (E1)
        ConsoleHelper.printStage("시스템", "필수항목을 검증합니다.");
        if (!preparation.validateRequiredFields()) {
            // E1) 필수 항목이 누락된 경우
            ConsoleHelper.printError("[E1] 필수 항목을 입력해주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        preparation.save();
        EducationPreparationDAO.save(preparation);

        // 10. 시스템은 등록 완료 결과를 출력한다.
        ConsoleHelper.printStage("시스템", "등록이 완료되었습니다.");
        ConsoleHelper.printInfo("등록번호: " + preparation.getSetupNumber()
                + " | 등록일시: " + preparation.getRegisteredAt()
                + " | 교육명: " + selectedPlan.getEducationName()
                + " | 교육장소: " + preparation.getLocation());

        // A2) 교육 진행이 필요한 경우
        boolean moveToExecution = ConsoleHelper.readYesNo("[영업교육담당자] 교육을 바로 진행하시겠습니까? (A2)");
        if (moveToExecution) {
            EducationExecutionRunner.run();
        }

        ConsoleHelper.waitEnter();
    }
}
