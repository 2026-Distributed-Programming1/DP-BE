package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.actor.Designer;
import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.old.dao.ConsultationRequestDAO;
import org.dpbe.old.dao.CustomerDAO;
import org.dpbe.old.dao.DesignerDAO;
import org.dpbe.old.runner.ConsoleHelper;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UC: 상담을 요청한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 고객은 [상담 신청] 메뉴를 클릭한다.
 *   2. 시스템은 상담 신청 화면을 출력한다. ([방문 상담] 기본값)
 *   3. 고객은 방문 상담 정보(희망일시, 방문장소, 연락처, 상담내용)를 입력한다. (A1, A2)
 *   4. 시스템은 유효성 검증 결과를 출력한다. (E1)
 *   5. 고객은 [신청] 버튼을 클릭한다.
 *   6. 시스템은 상담 신청 접수 결과(접수번호, 접수일시, 담당 설계사 정보)를 출력한다.
 *   7. 판매채널은 [신규 상담 신청 알림]을 클릭한다.
 *   8. 시스템은 상담 신청 상세 내용을 출력한다.
 *   9. 판매채널은 [상담 수락] 버튼을 클릭한다.
 *  10. 시스템은 상담 수락 완료 결과를 고객에게 출력한다.
 *
 * Alternative:
 *   A1) 전화 상담을 선택한 경우 → 연락 가능 시간대, 연락처, 상담내용 입력
 *   A2) 온라인 상담을 선택한 경우 → 희망 채널, 연락처, 상담내용 입력
 *
 * Exception:
 *   E1) 필수 항목(연락처, 상담 유형)이 입력되지 않은 경우 → 오류 메시지 출력
 */
public class ConsultationRequestRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 상담을 요청한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        // 2. 시스템은 상담 신청 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "상담 신청 화면을 출력합니다. (기본값: 방문 상담)");
        ConsultationRequest request = new ConsultationRequest();

        // 3. 고객은 상담 유형을 선택한다. (A1, A2)
        int typeChoice = ConsoleHelper.readMenuChoice(
                "[고객] 상담 유형을 선택하세요.",
                "방문 상담", "전화 상담 (A1)", "온라인 상담 (A2)");
        String type = switch (typeChoice) {
            case 1 -> "방문";
            case 2 -> "전화";
            case 3 -> "온라인";
            default -> "방문";
        };
        request.selectType(type);

        ConsoleHelper.printStage("고객", type + " 상담 정보를 입력합니다.");
        String contact = ConsoleHelper.readNonEmpty("  연락처 (000-0000-0000): ");
        String content = ConsoleHelper.readNonEmpty("  상담 내용: ");
        String location = "";
        LocalDateTime scheduledAt;
        if (type.equals("방문")) {
            location = ConsoleHelper.readNonEmpty("  희망 방문 장소: ");
            scheduledAt = ConsoleHelper.readDateTime("  희망 방문일시");
        } else {
            scheduledAt = ConsoleHelper.readDateTime("  희망 상담일시");
        }
        request.enterConsultationInfo(scheduledAt, location, contact, content);

        // 4. 시스템은 유효성 검증 결과를 출력한다. (E1)
        if (!request.validateRequiredFields()) {
            // E1) 필수 항목이 입력되지 않은 경우
            ConsoleHelper.printError("[E1] 필수 항목을 입력해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 담당 설계사 사전 배정 (step 6 출력에 포함)
        List<Designer> designers = DesignerDAO.findAll();
        if (designers.isEmpty()) {
            ConsoleHelper.printError("등록된 설계사가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        Designer designer = designers.get(0);

        // 5. 고객은 [신청] 버튼을 클릭한다.
        request.submit();
        ConsultationRequestDAO.save(request);

        // 6. 시스템은 상담 신청 접수 결과를 출력한다.
        ConsoleHelper.printStage("시스템", "상담 신청 접수 결과를 출력합니다.");
        ConsoleHelper.printInfo("접수번호: " + request.getConsultationNumber()
                + " | 접수일시: " + request.getReceivedAt()
                + " | 담당 설계사: " + designer.getName()
                + " | 상담유형: " + request.getType()
                + " | 상태: " + request.getStatus());
        ConsoleHelper.printStage("시스템", "판매채널(" + designer.getName() + ")에게 신규 상담 신청 알림을 발송합니다.");
        ConsoleHelper.printInfo("고객명: " + customer.getName()
                + " | 상담유형: " + request.getType()
                + " | 상담내용: " + request.getContent());

        // 9. 판매채널은 [상담 수락] 버튼을 클릭한다.
        designer.acceptConsultation(request);
        ConsultationRequestDAO.save(request);

        // 10. 시스템은 상담 수락 완료 결과를 고객에게 출력한다.
        ConsoleHelper.printStage("시스템", "상담 수락 완료 결과를 고객에게 출력합니다.");
        ConsoleHelper.printInfo("담당자명: " + designer.getName()
                + " | 수락일시: " + request.getAcceptedAt()
                + " | 예정 상담일시: " + request.getScheduledAt()
                + " | 상태: " + request.getStatus());

        ConsoleHelper.waitEnter();
    }

    private static Customer selectCustomer() {
        List<Customer> customers = CustomerDAO.findAll();
        if (customers.isEmpty()) {
            ConsoleHelper.printError("등록된 고객이 없습니다.");
            return null;
        }
        String[] options = customers.stream()
                .map(c -> c.getName() + " (" + c.getCustomerNo() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 상담을 요청할 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }
}
