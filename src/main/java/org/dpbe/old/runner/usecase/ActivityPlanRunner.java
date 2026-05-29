package org.dpbe.old.runner.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.actor.Agency;
import org.dpbe.domain.actor.Designer;
import org.dpbe.old.dao.ActivityPlanDAO;
import org.dpbe.old.dao.AgencyDAO;
import org.dpbe.old.dao.DesignerDAO;
import org.dpbe.domain.common.enums.ActivityType;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.domain.sales.entity.ActivityPlan;
import org.dpbe.domain.sales.entity.ScheduleItem;

/**
 * UC: 활동 계획을 작성한다
 *
 * Basic Path:
 *   1. 판매채널은 [영업 활동] 메뉴 > [활동 계획 작성] 항목을 클릭한다.
 *   2. 시스템은 활동 계획 작성 화면을 출력한다.
 *      (계획명/시작일/종료일/목표계약건수/목표계약금액 필수,
 *       목표신규고객수/메모/제안사유 선택, 제안대상고객번호/제안보험종류 필수)
 *   3. 판매채널은 활동 계획 정보를 입력한다. (A1, A2)
 *   4. 판매채널은 [제출] 버튼을 클릭한다. (A3)
 *   5. 시스템은 입력된 정보의 필수 항목 누락 여부를 검증한다. (E1)
 *   6. 시스템은 활동 계획을 저장하고 영업 관리자에게 검토 요청 알림을 발송한다.
 *   7. 시스템은 "활동 계획이 제출되었습니다. 영업 관리자의 검토 후 확정됩니다." 팝업을 출력한다.
 *   8. 해당 계획의 상태는 [검토 중]으로 표시된다.
 *
 * Alternative:
 *   A1) 종료일을 시작일보다 앞으로 설정할 경우
 *       → "종료일은 시작일보다 이전일 수 없습니다. 다시 선택해주세요." 팝업 출력
 *   A2) 이미 일정이 작성되어 있는 경우
 *       → [일정 추가] 버튼으로 일정 항목 추가
 *   A3) 임시저장이 필요한 경우
 *       → "임시저장되었습니다." 메시지 출력 → Basic Path 2번으로 돌아간다.
 *
 * Exception:
 *   E1) 필수 항목이 누락된 경우
 *       → "필수 항목을 확인해주세요." 팝업 출력
 *       → 누락 필드 강조 및 "필수 항목입니다." 문구 표시
 *       → 나머지 값 유지 → Basic Path 8번으로 복귀
 */
public class ActivityPlanRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 활동 계획을 작성한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 판매채널은 [활동 계획 작성] 항목을 클릭한다.
        ConsoleHelper.printStage("판매채널", "[영업 활동] 메뉴 > [활동 계획 작성] 항목을 클릭합니다.");

        ActivityPlan plan = new ActivityPlan();

        // 2. 시스템은 활동 계획 작성 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "활동 계획 작성 화면을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 계획명(필수) / 시작일(필수) / 종료일(필수)");
        ConsoleHelper.printInfo("          목표계약건수(필수) / 목표계약금액(필수) / 목표신규고객수(선택)");
        ConsoleHelper.printInfo("          제안대상고객번호(필수) / 제안보험종류(필수) / 제안사유(선택) / 메모(선택)");

        // 3. 판매채널은 활동 계획 정보를 입력한다.
        ConsoleHelper.printStage("판매채널", "활동 계획 정보를 입력합니다.");

        // 작성자 자동입력
        List<Agency> agencies = AgencyDAO.findAll();
        List<Designer> designers = DesignerDAO.findAll();
        String author = agencies.isEmpty()
                ? (designers.isEmpty() ? "판매채널" : designers.get(0).getName())
                : agencies.get(0).getName();
        plan.setAuthor(author);
        ConsoleHelper.printInfo("  작성자 (자동입력): " + author);

        // 계획명 (필수)
        String planName = ConsoleHelper.readNonEmpty("  계획명: ");
        plan.setPlanName(planName);

        // 시작일 / 종료일 (필수) - A1) 기간 유효성 검증
        plan.openCalendar();
        ConsoleHelper.printInfo("  [달력/시간 팝업] 계획 기간을 입력합니다.");
        LocalDate startDate;
        LocalDate endDate;
        while (true) {
            startDate = ConsoleHelper.readDate("  시작일");
            endDate = ConsoleHelper.readDate("  종료일");
            plan.setStartDate(startDate);
            plan.setEndDate(endDate);
            if (!plan.validateDateRange()) {
                // A1) 종료일이 시작일보다 앞인 경우
                plan.showDateRangeError();
                ConsoleHelper.printStage("시스템", "종료일은 시작일보다 이전일 수 없습니다. 다시 선택해주세요.");
            } else {
                break;
            }
        }

        // 목표 계약 건수 (필수)
        int targetContractCount = ConsoleHelper.readPositiveInt("  목표 계약 건수: ");
        plan.setTargetContractCount(targetContractCount);

        // 목표 계약 금액 (필수)
        long targetContractAmount = ConsoleHelper.readLong("  목표 계약 금액 (원): ");
        plan.setTargetContractAmount(targetContractAmount);

        // 목표 신규 고객 수 (선택)
        String newCustomerInput = ConsoleHelper.readLine("  목표 신규 고객 수 (없으면 엔터): ");
        if (!newCustomerInput.isEmpty()) {
            plan.setTargetNewCustomer(Integer.parseInt(newCustomerInput));
        }

        // 제안 대상 고객번호 (필수)
        String proposedCustomerId = ConsoleHelper.readNonEmpty("  제안 대상 고객번호: ");
        plan.setProposedCustomerId(proposedCustomerId);

        // 제안 보험 종류 (필수)
        int insuranceChoice = ConsoleHelper.readMenuChoice(
                "  제안 보험 종류를 선택하세요. (필수)",
                "생명", "건강", "자동차", "화재");
        switch (insuranceChoice) {
            case 1: plan.setProposedInsuranceType(InsuranceType.LIFE); break;
            case 2: plan.setProposedInsuranceType(InsuranceType.HEALTH); break;
            case 3: plan.setProposedInsuranceType(InsuranceType.AUTO); break;
            default: plan.setProposedInsuranceType(InsuranceType.FIRE); break;
        }

        // 제안 사유 (선택)
        String proposalReason = ConsoleHelper.readLine("  제안 사유 (없으면 엔터): ");
        if (!proposalReason.isEmpty()) {
            plan.setProposalReason(proposalReason);
        }

        // 메모 (선택)
        String memo = ConsoleHelper.readLine("  메모 (없으면 엔터): ");
        if (!memo.isEmpty()) {
            plan.setMemo(memo);
        }

        // A2) 일정 추가
        plan.sortSchedules();
        boolean addSchedule = ConsoleHelper.readYesNo("  [A2] 일정을 추가하시겠습니까?");
        while (addSchedule) {
            ConsoleHelper.printStage("시스템", "[일정 추가] 버튼이 배치됩니다. 일정 정보를 입력하세요.");
            String scheduleCustomerId = ConsoleHelper.readNonEmpty("    일정 고객번호: ");
            int actTypeChoice = ConsoleHelper.readMenuChoice(
                    "    활동 유형을 선택하세요.",
                    "방문", "상담", "전화");
            ActivityType actType;
            switch (actTypeChoice) {
                case 1: actType = ActivityType.VISIT; break;
                case 2: actType = ActivityType.CONSULTATION; break;
                default: actType = ActivityType.CALL; break;
            }
            LocalDateTime activityDateTime = ConsoleHelper.readDateTime("    활동 일시");
            String scheduleLocation = ConsoleHelper.readLine("    장소 (없으면 엔터): ");
            String scheduleMemo = ConsoleHelper.readLine("    메모 (없으면 엔터): ");
            ScheduleItem item = new ScheduleItem(scheduleCustomerId, actType, activityDateTime, scheduleLocation, scheduleMemo);
            plan.addSchedule(item);
            ConsoleHelper.printSuccess("일정이 추가되었습니다.");
            addSchedule = ConsoleHelper.readYesNo("  일정을 추가로 등록하시겠습니까?");
        }

        // 4. 판매채널은 [제출] 또는 [임시저장]을 클릭한다. (A3)
        int action = ConsoleHelper.readMenuChoice(
                "[판매채널] 처리를 선택하세요.",
                "제출", "임시저장");

        if (action == 2) {
            // A3) 임시저장이 필요한 경우
            plan.tempSave();
            ActivityPlanDAO.save(plan);
            plan.showTempSaveMessage();
            ConsoleHelper.printStage("시스템", "임시저장되었습니다.");
            ConsoleHelper.printInfo("계획ID: " + plan.getPlanId() + " | 상태: 임시저장");
            ConsoleHelper.printInfo("[A3] Basic Path 2번으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 5. 시스템은 필수 항목 누락 여부를 검증한다. (E1)
        ConsoleHelper.printStage("시스템", "필수 항목 누락 여부를 검증합니다.");
        if (!plan.validateRequired()) {
            // E1) 필수 항목 누락
            plan.highlightError();
            plan.retainValues();
            ConsoleHelper.printStage("시스템", "필수 항목을 확인해주세요.");
            ConsoleHelper.printError("[E1] 누락된 필드: 필수 항목입니다.");
            ConsoleHelper.printInfo("입력된 나머지 항목의 값은 유지됩니다. [제출] 버튼을 다시 클릭해주세요.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("필수 항목 검증 완료.");

        // 6. 시스템은 활동 계획을 저장하고 영업 관리자에게 검토 요청 알림을 발송한다.
        plan.submit();
        plan.notifyManager();
        ActivityPlanDAO.save(plan);
        ConsoleHelper.printStage("시스템", "활동 계획을 저장하고 영업 관리자에게 검토 요청 알림을 발송합니다.");

        // 7. 시스템은 "활동 계획이 제출되었습니다." 팝업 메시지를 출력한다.
        plan.showSubmitMessage();
        ConsoleHelper.printStage("시스템", "활동 계획이 제출되었습니다. 영업 관리자의 검토 후 확정됩니다.");

        // 8. 해당 계획의 상태는 [검토 중]으로 표시된다.
        ConsoleHelper.printStage("시스템", "계획 상태를 출력합니다.");
        String insuranceTypeStr;
        switch (plan.getProposedInsuranceType()) {
            case LIFE: insuranceTypeStr = "생명"; break;
            case HEALTH: insuranceTypeStr = "건강"; break;
            case AUTO: insuranceTypeStr = "자동차"; break;
            default: insuranceTypeStr = "화재"; break;
        }
        ConsoleHelper.printInfo("계획ID: " + plan.getPlanId()
                + " | 계획명: " + plan.getPlanName()
                + " | 기간: " + plan.getStartDate() + " ~ " + plan.getEndDate()
                + " | 목표계약: " + plan.getTargetContractCount() + "건 / " + plan.getTargetContractAmount() + "원"
                + " | 제안보험: " + insuranceTypeStr
                + " | 일정수: " + plan.getSchedules().size() + "건"
                + " | 상태: " + (plan.getStatus() == PlanStatus.UNDER_REVIEW ? "검토 중" : "임시저장"));

        ConsoleHelper.waitEnter();
    }
}