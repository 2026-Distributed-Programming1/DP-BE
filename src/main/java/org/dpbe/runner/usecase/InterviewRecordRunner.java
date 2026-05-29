package org.dpbe.runner.usecase;

import org.dpbe.consultation.InterviewRecord;
import org.dpbe.dao.InterviewRecordDAO;
import org.dpbe.runner.ConsoleHelper;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC: 면담기록을 관리한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 판매채널은 [면담기록 관리] 메뉴를 클릭한다.
 *   2. 시스템은 면담기록 관리 화면을 출력한다.
 *   3. 판매채널은 조회 조건을 입력하고 [조회] 버튼을 클릭한다. (A1)
 *   4. 시스템은 조회 조건에 맞는 면담기록 목록을 출력한다. (A2)
 *   5. 판매채널은 면담 항목을 클릭한다.
 *   6. 시스템은 상세 정보와 [수정]/[닫기] 버튼을 출력한다.
 *   7. 판매채널은 [닫기] 버튼을 클릭한다. (A3)
 *   8. 시스템은 면담기록 목록 화면으로 돌아간다.
 *
 * Alternative:
 *   A1) [새 기록 등록] 버튼을 클릭한 경우 → 기록 등록 화면 출력
 *       → 저장 후 [보험상품 제안] 버튼 클릭 시 보험상품을 제안한다 유스케이스로 이동
 *   A2) 조회 조건에 맞는 면담기록이 없는 경우 → 안내 메시지 출력
 *   A3) [수정] 버튼을 클릭한 경우 → 면담 기록 수정 (E2)
 *
 * Exception:
 *   E1) 등록 시 필수 항목(고객명, 면담일시, 면담 내용)이 입력되지 않은 경우
 *   E2) 수정 시 필수 항목(면담 내용)이 입력되지 않은 경우
 */
public class InterviewRecordRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 면담기록을 관리한다");
        ConsoleHelper.printDoubleDivider();

        // 2. 시스템은 면담기록 관리 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "면담기록 관리 화면을 출력합니다.");
        List<InterviewRecord> interviewRecords = InterviewRecordDAO.findAll();
        if (!interviewRecords.isEmpty()) {
            ConsoleHelper.printInfo("면담기록 목록 (고객명 / 면담일시):");
            for (InterviewRecord r : interviewRecords) {
                ConsoleHelper.printInfo("[" + r.getRecordNumber() + "] "
                        + r.getCustomerName()
                        + " | " + r.getInterviewedAt());
            }
        } else {
            ConsoleHelper.printInfo("등록된 면담 기록이 없습니다.");
        }

        // 3. 판매채널은 작업을 선택한다. (A1, A3)
        int action = ConsoleHelper.readMenuChoice(
                "[판매채널] 작업을 선택하세요.",
                "새 기록 등록 (A1)", "기록 수정 (A3)", "닫기");

        if (action == 3) {
            ConsoleHelper.printStage("시스템", "면담기록 목록 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        if (action == 1) {
            // A1) [새 기록 등록] 버튼을 클릭한 경우
            ConsoleHelper.printStage("시스템", "면담기록 등록 화면을 출력합니다.");
            InterviewRecord record = new InterviewRecord();

            String customerName = ConsoleHelper.readNonEmpty("  고객명: ");
            record.setCustomerName(customerName);

            LocalDateTime interviewedAt = ConsoleHelper.readDateTime("  면담일시");
            record.setInterviewedAt(interviewedAt);

            String content = ConsoleHelper.readNonEmpty("  면담 내용: ");
            String reaction = ConsoleHelper.readNonEmpty("  고객 반응: ");
            String followUp = ConsoleHelper.readLine("  후속 조치 (없으면 엔터): ");

            // E1) 필수 항목 검증 (고객명, 면담일시, 면담 내용)
            if (customerName.isEmpty() || interviewedAt == null || content.isEmpty()) {
                ConsoleHelper.printError("[E1] 필수 항목을 입력해 주세요. (고객명/면담일시/면담 내용)");
                ConsoleHelper.waitEnter();
                return;
            }

            record.save(content, reaction, followUp);
            InterviewRecordDAO.save(record);

            // 5. 시스템은 저장 완료 결과를 출력한다.
            ConsoleHelper.printStage("시스템", "면담 기록 저장 완료 결과를 출력합니다.");
            ConsoleHelper.printInfo("기록번호: " + record.getRecordNumber()
                    + " | 저장일시: " + record.getRecordedAt()
                    + " | 고객명: " + record.getCustomerName()
                    + " | 면담일시: " + record.getInterviewedAt());

            // 6. 판매채널은 [보험상품 제안] 버튼을 클릭한다.
            boolean moveToProposal = ConsoleHelper.readYesNo(
                    "[판매채널] 보험상품 제안 화면으로 이동하시겠습니까?");
            if (moveToProposal) {
                // 시스템은 보험상품을 제안한다 유스케이스로 이동한다.
                ProposalRunner.run(record);
            }

        } else {
            // A3) [수정] 버튼을 클릭한 경우
            if (interviewRecords.isEmpty()) {
                // A2) 면담기록이 없는 경우
                ConsoleHelper.printError("[A2] 조회된 면담 기록이 없습니다.");
                ConsoleHelper.waitEnter();
                return;
            }
            String[] recordOptions = interviewRecords.stream()
                    .map(r -> "[" + r.getRecordNumber() + "] " + r.getCustomerName()
                            + " | " + r.getInterviewedAt())
                    .toArray(String[]::new);
            int idx = ConsoleHelper.readMenuChoice("수정할 면담 기록을 선택하세요.", recordOptions);
            ConsoleHelper.printStage("시스템", "면담 기록을 편집 가능한 상태로 출력합니다.");
            InterviewRecord record = interviewRecords.get(idx - 1);

            String content = ConsoleHelper.readNonEmpty("  수정할 면담 내용: ");
            String reaction = ConsoleHelper.readNonEmpty("  고객 반응: ");
            String followUp = ConsoleHelper.readLine("  후속 조치 (없으면 엔터): ");

            // E2) 수정 시 필수 항목 검증
            if (content.isEmpty()) {
                ConsoleHelper.printError("[E2] 필수 항목을 입력해 주세요.");
                ConsoleHelper.waitEnter();
                return;
            }

            record.modify(content, reaction, followUp);
            InterviewRecordDAO.save(record);
            ConsoleHelper.printStage("시스템", "수정 완료 결과를 출력합니다.");
            ConsoleHelper.printInfo("기록번호: " + record.getRecordNumber()
                    + " | 수정일시: " + record.getModifiedAt());
        }

        ConsoleHelper.waitEnter();
    }
}
