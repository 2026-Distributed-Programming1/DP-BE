package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.Designer;
import org.dpbe.domain.consultation.entity.InterviewSchedule;
import org.dpbe.old.dao.DesignerDAO;
import org.dpbe.old.dao.InterviewScheduleDAO;
import org.dpbe.old.runner.ConsoleHelper;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC: 면담일정을 관리한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 판매채널은 [면담일정 관리] 메뉴를 선택한다.
 *   2. 시스템은 면담일정 관리 화면을 출력한다. (오늘 날짜 기준 예정 목록)
 *   3. 판매채널은 조회 조건을 입력하고 [조회] 버튼을 클릭한다. (A1)
 *   4. 시스템은 조회 조건에 맞는 면담일정 목록을 출력한다. (A6)
 *   5. 판매채널은 면담 항목을 클릭한다.
 *   6. 시스템은 선택한 면담의 상세 정보 화면을 출력한다.
 *   7. 판매채널은 [닫기] 버튼을 클릭한다. (A4, A5)
 *   8. 시스템은 면담일정 목록 화면으로 돌아간다.
 *
 * Alternative:
 *   A1) [새 면담 등록] 버튼을 클릭한 경우 → 면담 등록 화면 출력 (A2, A3)
 *   A2) 전화 면담을 선택한 경우 → 연락 가능 시간대, 준비사항 입력
 *   A3) 온라인 면담을 선택한 경우 → 희망 채널, 면담일시, 준비사항 입력
 *   A4) [수정] 버튼을 클릭한 경우 → 면담 정보 수정 (E2)
 *   A5) [취소] 버튼을 클릭한 경우 → 면담 상태를 취소로 변경
 *   A6) 조회 조건에 맞는 면담 일정이 없는 경우 → 안내 메시지 출력
 *
 * Exception:
 *   E1) 등록 시 필수 항목(고객명, 면담유형, 면담일시)이 입력되지 않은 경우
 *   E2) 수정 시 필수 항목(면담유형, 면담일시)이 입력되지 않은 경우
 */
public class InterviewScheduleRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 면담일정을 관리한다");
        ConsoleHelper.printDoubleDivider();

        // 2. 시스템은 면담일정 관리 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "면담일정 관리 화면을 출력합니다.");
        List<InterviewSchedule> interviewSchedules = InterviewScheduleDAO.findAll();
        if (!interviewSchedules.isEmpty()) {
            ConsoleHelper.printInfo("면담 목록 (고객명 / 유형 / 상태):");
            for (InterviewSchedule s : interviewSchedules) {
                ConsoleHelper.printInfo("[" + s.getInterviewNumber() + "] "
                        + s.getCustomerName()
                        + " | " + s.getType()
                        + " | " + s.getStatus());
            }
        } else {
            ConsoleHelper.printInfo("등록된 면담 일정이 없습니다.");
        }

        // 3. 판매채널은 작업을 선택한다. (A1)
        int action = ConsoleHelper.readMenuChoice(
                "[판매채널] 작업을 선택하세요.",
                "새 면담 등록 (A1)", "면담 항목 선택", "닫기");

        if (action == 3) {
            // 7~8. [닫기] → 목록 화면으로 복귀
            ConsoleHelper.printStage("시스템", "면담일정 목록 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        if (action == 1) {
            // A1) [새 면담 등록] 버튼을 클릭한 경우
            ConsoleHelper.printStage("시스템", "면담 등록 화면을 출력합니다.");
            InterviewSchedule schedule = new InterviewSchedule();

            String customerName = ConsoleHelper.readNonEmpty("  고객명: ");

            // 담당 설계사 배정
            List<Designer> designers = DesignerDAO.findAll();
            String designerName = "";
            if (!designers.isEmpty()) {
                String[] designerOptions = designers.stream()
                        .map(Designer::getName).toArray(String[]::new);
                int didx = ConsoleHelper.readMenuChoice("  담당 설계사를 선택하세요.", designerOptions);
                designerName = designers.get(didx - 1).getName();
            } else {
                designerName = ConsoleHelper.readNonEmpty("  담당 설계사명: ");
            }
            schedule.setDesignerName(designerName);

            // A2, A3 분기
            int typeChoice = ConsoleHelper.readMenuChoice(
                    "[판매채널] 면담 유형을 선택하세요.",
                    "방문 면담", "전화 면담 (A2)", "온라인 면담 (A3)");
            String type = switch (typeChoice) {
                case 1 -> "방문";
                case 2 -> "전화";
                case 3 -> "온라인";
                default -> "방문";
            };
            schedule.setType(type);

            LocalDateTime scheduledAt = ConsoleHelper.readDateTime("  희망 면담일시");
            String location = type.equals("방문")
                    ? ConsoleHelper.readNonEmpty("  면담 장소: ")
                    : "";
            String preparation = ConsoleHelper.readLine("  면담 준비사항 (없으면 엔터): ");

            // E1) 필수 항목 검증
            if (customerName.isEmpty() || type.isEmpty() || scheduledAt == null) {
                ConsoleHelper.printError("[E1] 필수 항목을 입력해 주세요.");
                ConsoleHelper.waitEnter();
                return;
            }

            schedule.register(customerName, scheduledAt, location, preparation);
            InterviewScheduleDAO.save(schedule);

            // A1 step 5. 시스템은 등록 완료 결과를 출력한다.
            ConsoleHelper.printStage("시스템", "면담 등록 완료 결과를 출력합니다.");
            ConsoleHelper.printInfo("면담번호: " + schedule.getInterviewNumber()
                    + " | 등록일시: " + schedule.getRegisteredAt()
                    + " | 고객명: " + schedule.getCustomerName()
                    + " | 담당 설계사: " + schedule.getDesignerName()
                    + " | 면담일시: " + schedule.getScheduledAt());

            // 알림 발송
            boolean sendNotice = ConsoleHelper.readYesNo("[판매채널] 고객에게 면담 일정 알림을 발송하시겠습니까?");
            if (sendNotice) {
                schedule.sendNotice();
            }

        } else if (action == 2) {
            // 5. 판매채널은 면담 항목을 클릭한다.
            if (interviewSchedules.isEmpty()) {
                ConsoleHelper.printError("조회된 면담 일정이 없습니다.");
                ConsoleHelper.waitEnter();
                return;
            }
            String[] options = interviewSchedules.stream()
                    .map(s -> "[" + s.getInterviewNumber() + "] " + s.getCustomerName()
                            + " | " + s.getType() + " | " + s.getScheduledAt())
                    .toArray(String[]::new);
            int idx = ConsoleHelper.readMenuChoice("면담 항목을 선택하세요.", options);
            InterviewSchedule schedule = interviewSchedules.get(idx - 1);

            // 6. 시스템은 선택한 면담의 상세 정보 화면을 출력한다.
            ConsoleHelper.printStage("시스템", "면담 상세 정보를 출력합니다.");
            ConsoleHelper.printInfo("면담번호: " + schedule.getInterviewNumber()
                    + " | 고객명: " + schedule.getCustomerName()
                    + " | 담당 설계사: " + schedule.getDesignerName());
            ConsoleHelper.printInfo("면담유형: " + schedule.getType()
                    + " | 면담일시: " + schedule.getScheduledAt()
                    + " | 면담장소: " + schedule.getLocation());
            ConsoleHelper.printInfo("준비사항: " + schedule.getPreparation()
                    + " | 상태: " + schedule.getStatus());

            // 7. 판매채널은 [닫기]/[수정]/[취소] 버튼을 클릭한다. (A4, A5)
            int subAction = ConsoleHelper.readMenuChoice(
                    "[판매채널] 작업을 선택하세요.",
                    "수정 (A4)", "취소 (A5)", "닫기");

            if (subAction == 1) {
                // A4) [수정] 버튼을 클릭한 경우
                ConsoleHelper.printStage("시스템", "면담 정보를 편집 가능한 상태로 출력합니다.");

                // SCH-03: 면담유형 재선택 추가
                int typeChoice = ConsoleHelper.readMenuChoice(
                        "  변경할 면담 유형을 선택하세요.",
                        "방문 면담", "전화 면담", "온라인 면담", "변경 없음");
                String newType = switch (typeChoice) {
                    case 1 -> "방문";
                    case 2 -> "전화";
                    case 3 -> "온라인";
                    default -> schedule.getType();
                };
                schedule.setType(newType);

                LocalDateTime scheduledAt = ConsoleHelper.readDateTime("  변경할 면담일시");
                String location = ConsoleHelper.readNonEmpty("  변경할 장소: ");
                String preparation = ConsoleHelper.readLine("  준비사항 (없으면 엔터): ");

                // E2) 수정 시 필수 항목 검증 (면담유형 + 면담일시)
                if (newType == null || newType.isEmpty() || scheduledAt == null) {
                    ConsoleHelper.printError("[E2] 필수 항목을 입력해 주세요. (면담유형, 면담일시)");
                    ConsoleHelper.waitEnter();
                    return;
                }
                schedule.modify(scheduledAt, location, preparation);
                InterviewScheduleDAO.save(schedule);

                ConsoleHelper.printStage("시스템", "수정 완료 결과를 출력합니다.");
                ConsoleHelper.printInfo("면담번호: " + schedule.getInterviewNumber()
                        + " | 수정일시: " + schedule.getModifiedAt());

            } else if (subAction == 2) {
                // A5) [취소] 버튼을 클릭한 경우
                boolean confirm = ConsoleHelper.readYesNo("  해당 면담을 취소하시겠습니까?");
                if (confirm) {
                    schedule.cancel();
                    InterviewScheduleDAO.save(schedule);
                    ConsoleHelper.printStage("시스템", "취소 완료 결과를 출력합니다.");
                    ConsoleHelper.printInfo("면담번호: " + schedule.getInterviewNumber()
                            + " | 취소일시: " + schedule.getCancelledAt());
                }
            } else {
                // 8. [닫기] → 목록 화면으로 복귀
                ConsoleHelper.printStage("시스템", "면담일정 목록 화면으로 돌아갑니다.");
            }
        }

        ConsoleHelper.waitEnter();
    }
}
