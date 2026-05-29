package org.dpbe.runner.usecase;

import org.dpbe.actor.EducationTrainer;
import org.dpbe.dao.EducationExecutionDAO;
import org.dpbe.dao.EducationPlanDAO;
import org.dpbe.dao.EducationPreparationDAO;
import org.dpbe.dao.EducationTrainerDAO;
import org.dpbe.db.DBA;
import org.dpbe.education.Attendance;
import java.util.List;
import org.dpbe.education.EducationExecution;
import org.dpbe.education.EducationPreparation;
import org.dpbe.runner.ConsoleHelper;

/**
 * UC: 교육을 진행한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 교육 제반을 등록한다 A2)에서 넘어온다.
 *   2. 시스템은 교육 진행 화면을 출력한다. (출석 대상자 목록 자동 로드)
 *   3. 영업교육 담당자는 출석 대상자별 출석 여부를 선택한다.
 *   4. 시스템은 출석 현황(출석인원/전체인원)을 실시간으로 출력한다.
 *   5. 영업교육 담당자는 메모를 입력하고 [진행 완료] 버튼을 클릭한다. (A1)
 *   6. 시스템은 교육 완료 확인 팝업을 출력한다. (E1)
 *   7. 영업교육 담당자는 [확인] 버튼을 클릭한다.
 *   8. 시스템은 판매채널에게 교육 수료 알림을 자동 발송한다. (E2)
 *   9. 영업교육 담당자는 [확인] 버튼을 클릭한다.
 *  10. 시스템은 교육 진행 완료 결과를 출력한다.
 *
 * Alternative:
 *   A1) [취소] 버튼을 클릭한 경우 → 교육 제반 등록 화면으로 복귀
 *
 * Exception:
 *   E1) 교육 완료 처리 실패 시 → 재시도 안내
 *   E2) 수료 알림 발송 실패 시 → 수동 안내 메시지 출력
 */
public class EducationExecutionRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 교육을 진행한다");
        ConsoleHelper.printDoubleDivider();

        List<EducationPreparation> educationPreparations = EducationPreparationDAO.findAll();
        if (educationPreparations.isEmpty()) {
            ConsoleHelper.printError("등록된 교육 제반이 없습니다. 먼저 교육 제반을 등록해주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        String[] prepOptions = educationPreparations.stream()
                .map(p -> "#" + p.getSetupNumber() + " - " + p.getLocation()
                        + " / 강사: " + p.getInstructorName())
                .toArray(String[]::new);
        int prepChoice = ConsoleHelper.readMenuChoice(
                "[영업교육담당자] 진행할 교육 제반을 선택하세요:", prepOptions);
        EducationPreparation preparation = educationPreparations.get(prepChoice - 1);
        List<EducationTrainer> trainerList = EducationTrainerDAO.findAll();
        if (trainerList.isEmpty()) {
            ConsoleHelper.printError("등록된 영업교육 담당자가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        EducationTrainer trainer = trainerList.get(0);
        EducationExecution execution = trainer.conductEducation(preparation);

        // 2. 시스템은 교육 진행 화면을 출력한다. (출석 대상자 명단 자동 로드)
        List<Attendance> list = execution.loadAttendanceList();
        ConsoleHelper.printStage("시스템", "교육 진행 화면을 출력합니다.");
        ConsoleHelper.printInfo("교육장소: " + preparation.getLocation()
                + " | 강사명: " + preparation.getInstructorName());
        ConsoleHelper.printStage("시스템", "출석 대상자 명단 (자동 로드):");
        for (Attendance a : list) {
            ConsoleHelper.printInfo("- " + a.getAttendeeName());
        }

        // 3. 영업교육 담당자는 출석 여부를 선택한다.
        ConsoleHelper.printStage("영업교육담당자", "출석 체크를 진행합니다.");
        for (Attendance a : list) {
            boolean attended = ConsoleHelper.readYesNo("  " + a.getAttendeeName() + " 출석");
            execution.markAttendance(a.getAttendeeName(), attended);
        }

        // 4. 시스템은 출석 현황을 실시간으로 출력한다.
        int count = execution.calculateAttendanceCount();
        ConsoleHelper.printStage("시스템", "출석 현황: " + count + " / " + list.size());

        // 5. 영업교육 담당자는 메모를 입력하고 [진행 완료] 버튼을 클릭한다. (A1)
        String memo = ConsoleHelper.readLine("  교육 진행 메모 (없으면 엔터): ");
        if (!memo.isEmpty()) execution.setMemo(memo);

        int action = ConsoleHelper.readMenuChoice(
                "[영업교육담당자] 처리를 선택하세요.",
                "진행 완료", "취소");

        if (action == 2) {
            // A1) [취소] 버튼을 클릭한 경우
            ConsoleHelper.printInfo("[A1] 교육 제반 등록 화면으로 복귀합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 6. 시스템은 교육 완료 처리 확인 팝업을 출력한다.
        ConsoleHelper.printStage("시스템", "교육을 완료 처리하시겠습니까?");
        boolean confirm = ConsoleHelper.readYesNo("  확인");
        if (!confirm) {
            ConsoleHelper.waitEnter();
            return;
        }

        execution.complete();
        DBA.beginTransaction();
        try {
            EducationExecutionDAO.save(execution);
            DBA.commit();
        } catch (Exception e) {
            DBA.rollback();
            ConsoleHelper.printError("교육 진행 저장 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 8. 시스템은 판매채널에게 수료 알림을 자동 발송한다.
        execution.sendCompletionNotice();

        // 10. 시스템은 교육 진행 완료 결과를 출력한다.
        String educationName = EducationPlanDAO.findAll().stream()
                .filter(p -> String.valueOf(p.getPlanNumber()).equals(preparation.getPlanNo()))
                .map(org.dpbe.education.EducationPlan::getEducationName)
                .findFirst().orElse("-");
        ConsoleHelper.printStage("시스템", "교육 진행 완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("완료번호: " + execution.getCompletionNumber()
                + " | 완료일시: " + execution.getCompletedAt()
                + " | 교육명: " + educationName
                + " | 출석인원: " + execution.getAttendanceCount()
                + " / " + execution.getTotalCount());

        ConsoleHelper.waitEnter();
    }
}
