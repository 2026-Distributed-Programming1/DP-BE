package org.dpbe.old.runner.usecase;

import java.time.LocalDate;
import java.util.List;
import org.dpbe.domain.actor.SalesManager;
import org.dpbe.old.dao.ChannelRecruitmentDAO;
import org.dpbe.old.dao.SalesManagerDAO;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.domain.sales.entity.ChannelRecruitment;

/**
 * UC: 판매채널을 모집한다 시나리오 진행자
 *
 * Basic Path:
 *   1. [영업 활동을 관리한다] A1)으로부터 넘어온다.
 *   2. 시스템은 채널 모집 화면을 출력한다.
 *      (화면 상단: 기존 모집 공고 목록 테이블 / 우측 상단: [신규 모집 등록], [닫기] 버튼)
 *   3. 영업 관리자는 [신규 모집 등록] 버튼을 클릭한다. (A1)
 *   4. 시스템은 신규 모집 등록 폼을 출력한다.
 *      (채널유형: 설계사/대리점 선택 필수 / 모집인원: 숫자 필수 / 모집기간: 시작일~종료일 필수 / 모집조건: 선택)
 *   5. 영업 관리자는 모집 정보를 입력하고 [저장] 버튼을 클릭한다. (A2, E1)
 *   6. 시스템은 "모집 공고가 등록되었습니다."라는 팝업 메시지를 출력한다.
 *   7. 영업 관리자는 [확인] 버튼을 클릭한다.
 *   8. 시스템은 등록 완료 결과(모집번호, 등록일시, 채널유형, 모집기간)를 출력하고
 *      [영업 활동을 관리한다] 유스케이스로 복귀한다.
 *
 * Alternative:
 *   A1) [닫기] 버튼을 클릭한 경우
 *       → 채널 모집 화면을 닫고 [영업 활동을 관리한다] 유스케이스로 복귀한다.
 *   A2) [취소] 버튼을 클릭한 경우
 *       → 취소 확인 팝업 출력 후 모집 등록 폼을 닫고 채널 모집 화면으로 돌아간다.
 *
 * Exception:
 *   E1) 필수 항목이 누락된 경우
 *       → 오류 메시지 출력 후 재입력 유도
 */
public class ChannelRecruitmentRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 판매채널을 모집한다");
        ConsoleHelper.printDoubleDivider();

        List<SalesManager> managers = SalesManagerDAO.findAll();
        if (managers.isEmpty()) {
            ConsoleHelper.printError("등록된 영업 관리자가 없습니다. 먼저 영업 관리자를 등록해주세요.");
            ConsoleHelper.waitEnter();
            return;
        }
        SalesManager manager = managers.get(0);

        // 1. [영업 활동을 관리한다] A1)으로부터 넘어온다.
        ConsoleHelper.printInfo("[영업 활동을 관리한다] A1) 인원 충당 필요로 채널 모집 화면으로 이동합니다.");

        // 2. 시스템은 채널 모집 화면을 출력한다.
        ChannelRecruitment recruitment = new ChannelRecruitment();
        recruitment.setManagerName(manager.getName());
        recruitment.loadRecruitmentList();
        ConsoleHelper.printStage("시스템", "채널 모집 화면을 출력합니다.");
        ConsoleHelper.printInfo(" ───────────────────────────────────────────────────────── ");
        ConsoleHelper.printInfo("   [기존 모집 공고 목록]                                      ");
        ConsoleHelper.printInfo("   모집번호 / 채널유형 / 모집인원 / 모집기간 / 등록일시            ");
        List<ChannelRecruitment> channelRecruitments = ChannelRecruitmentDAO.findAll();
        if (channelRecruitments.isEmpty()) {
            ConsoleHelper.printInfo("   (등록된 모집 공고가 없습니다.)                               ");
        } else {
            for (ChannelRecruitment r : channelRecruitments) {
                String type = r.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점";
                ConsoleHelper.printInfo("│  " + r.getRecruitmentNo()
                        + " | " + type
                        + " | " + r.getRecruitCount() + "명"
                        + " | " + r.getStartDate() + "~" + r.getEndDate()
                        + " | " + r.getRegisteredAt());
            }
        }
        ConsoleHelper.printInfo("└─────────────────────────────────────────────────────────┘");
        ConsoleHelper.printInfo("[우측 상단] [신규 모집 등록] [닫기] 버튼");

        // 3. 영업 관리자는 [신규 모집 등록] 또는 [닫기]를 클릭한다. (A1)
        int topAction = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "신규 모집 등록", "닫기");

        if (topAction == 2) {
            // A1) [닫기] 버튼을 클릭한 경우
            recruitment.close();
            ConsoleHelper.printInfo("[A1] 채널 모집 화면을 닫습니다.");
            recruitment.returnToActivityManagement();
            ConsoleHelper.waitEnter();
            return;
        }

        // 4. 시스템은 신규 모집 등록 폼을 출력한다.
        recruitment.openRegistrationForm();
        ConsoleHelper.printStage("시스템", "신규 모집 등록 폼을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 채널유형(필수) / 모집인원(필수) / 모집기간(필수) / 모집조건(선택)");

        // 5. 영업 관리자는 모집 정보를 입력하고 [저장] 버튼을 클릭한다. (A2, E1)
        LocalDate startDate;
        LocalDate endDate;
        while (true) {
            ConsoleHelper.printStage("영업관리자", "모집 정보를 입력합니다.");

            // 채널유형 선택 (필수)
            int typeChoice = ConsoleHelper.readMenuChoice(
                    "  채널유형을 선택하세요. (필수)",
                    "설계사", "대리점");
            recruitment.setChannelType(typeChoice == 1 ? ChannelType.DESIGNER : ChannelType.AGENCY);

            // 모집인원 입력 (필수)
            int count = ConsoleHelper.readPositiveInt("  모집인원 (명): ");
            recruitment.setRecruitCount(count);

            // 모집기간 입력 (필수) - 달력 팝업 대체
            recruitment.openCalendar();
            ConsoleHelper.printInfo("  [달력 팝업] 모집 기간을 입력합니다.");
            startDate = ConsoleHelper.readDate("  모집 기간 시작일");
            endDate = ConsoleHelper.readDate("  모집 기간 종료일");
            recruitment.setStartDate(startDate);
            recruitment.setEndDate(endDate);

            // 모집조건 입력 (선택)
            String condition = ConsoleHelper.readLine("  모집조건 (경력, 자격증 등, 없으면 엔터): ");
            if (!condition.isEmpty()) {
                recruitment.setCondition(condition);
            }

            // [저장] 또는 [취소] 선택 (A2)
            int saveAction = ConsoleHelper.readMenuChoice(
                    "[영업관리자] 처리를 선택하세요.",
                    "저장", "취소");

            if (saveAction == 2) {
                // A2) [취소] 버튼을 클릭한 경우
                recruitment.showCancelConfirm();
                ConsoleHelper.printStage("시스템", "작성 중인 내용이 저장되지 않습니다. 취소하시겠습니까?");
                boolean confirmCancel = ConsoleHelper.readYesNo("  확인");
                if (confirmCancel) {
                    recruitment.cancel();
                    ConsoleHelper.printInfo("[A2] 모집 등록 폼을 닫고 채널 모집 화면으로 돌아갑니다.");
                }
                ConsoleHelper.waitEnter();
                return;
            }

            // E1) 필수 항목 누락 검증
            ConsoleHelper.printStage("시스템", "필수 항목 누락 여부를 검증합니다.");
            if (!recruitment.validateRequired()) {
                recruitment.highlightError();
                recruitment.showRequiredError();
                ConsoleHelper.printError("[E1] 필수 항목을 입력해주세요. (채널유형 / 모집인원 / 모집기간)");
                continue;
            }

            // 모집기간 유효성 검증
            if (endDate.isBefore(startDate)) {
                ConsoleHelper.printError("[E1] 종료일은 시작일보다 이전일 수 없습니다. 다시 입력해주세요.");
                continue;
            }

            ConsoleHelper.printSuccess("필수 항목 검증 완료.");
            break;
        }

        // 저장 처리
        recruitment.save();

        // 6. 시스템은 "모집 공고가 등록되었습니다."라는 팝업 메시지를 출력한다.
        ChannelRecruitmentDAO.save(recruitment);
        recruitment.showSaveSuccess();
        ConsoleHelper.printStage("시스템", "모집 공고가 등록되었습니다.");

        // 7. 영업 관리자는 [확인] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[확인] 버튼을 클릭합니다.");
        ConsoleHelper.readLine("  [확인] (엔터를 눌러 확인): ");

        // 8. 시스템은 등록 완료 결과를 출력하고 [영업 활동을 관리한다]로 복귀한다.
        recruitment.showSaveResult();
        ConsoleHelper.printStage("시스템", "등록 완료 결과를 출력합니다.");
        String channelTypeStr = recruitment.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점";
        ConsoleHelper.printInfo("모집번호: " + recruitment.getRecruitmentNo()
                + " | 등록일시: " + recruitment.getRegisteredAt()
                + " | 채널유형: " + channelTypeStr
                + " | 모집기간: " + recruitment.getStartDate() + " ~ " + recruitment.getEndDate());
        recruitment.returnToActivityManagement();
        ConsoleHelper.printInfo("[영업 활동을 관리한다] 유스케이스로 복귀합니다.");

        ConsoleHelper.waitEnter();
    }
}