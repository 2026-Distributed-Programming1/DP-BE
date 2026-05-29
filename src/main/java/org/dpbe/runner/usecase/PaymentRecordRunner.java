package org.dpbe.runner.usecase;

import org.dpbe.dao.OverdueNoticeSettingDAO;
import org.dpbe.dao.PaymentRecordDAO;
import org.dpbe.enums.PaymentRecordStatus;
import org.dpbe.enums.RejectCategory;
import org.dpbe.payment.OverdueNoticeSetting;
import org.dpbe.payment.PaymentRecord;
import org.dpbe.runner.ConsoleHelper;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC8: 「납부 내역을 관리한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 납부 내역 조회 → 선택 → 수납 확정
 * 분기:
 * - A3: 수납 반려
 * - 부가: 미납 알림 자동 발송 설정
 * - E1: 확정 처리 오류
 */
public class PaymentRecordRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC8: 납부 내역을 관리한다");
        ConsoleHelper.printDoubleDivider();

        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[재무회계 담당자] 작업을 선택하세요:",
                    "납부 내역 조회 및 처리",
                    "미납 알림 자동 발송 설정",
                    "이전 메뉴로");
            if (choice == 1) {
                processRecords();
            } else if (choice == 2) {
                configureOverdueNotice();
            } else {
                return;
            }
        }
    }

    private static void processRecords() {
        List<PaymentRecord> waiting = PaymentRecordDAO.findAll().stream()
                .filter(r -> r.getStatus() == PaymentRecordStatus.WAITING)
                .collect(Collectors.toList());

        if (waiting.isEmpty()) {
            ConsoleHelper.printInfo("처리할 납부 내역(대기 상태)이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        String[] options = waiting.stream()
                .map(r -> r.getRecordNo() + " - 계약 " + r.getContract().getContractNo()
                        + ", 금액 " + r.getAmount() + "원, " + r.getMethod())
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 처리할 납부 내역을 선택하세요:", options);
        PaymentRecord record = waiting.get(choice - 1);

        record.load();
        showRecord(record);

        // 처리 선택
        int actionChoice = ConsoleHelper.readMenuChoice(
                "[재무회계 담당자] 처리 방법을 선택하세요:",
                "수납 확정 (정상)",
                "수납 반려 (A3)",
                "취소");
        if (actionChoice == 1) {
            record.confirm();
            PaymentRecordDAO.save(record);
            ConsoleHelper.printSuccess("수납 확정 완료: " + record.getRecordNo());
        } else if (actionChoice == 2) {
            handleReject(record);
        }
        ConsoleHelper.waitEnter();
    }

    /** A3: 수납 반려 */
    private static void handleReject(PaymentRecord record) {
        ConsoleHelper.printStage("재무회계 담당자", "[A3] 수납 반려를 진행합니다.");
        int catChoice = ConsoleHelper.readMenuChoice("반려 분류를 선택하세요:",
                "오류결제", "이중납부", "계약불일치", "기타");
        RejectCategory cat = RejectCategory.values()[catChoice - 1];
        String reason = ConsoleHelper.readNonEmpty("  상세 반려 사유: ");
        record.enterRejectInfo(cat, reason);
        record.reject();
        PaymentRecordDAO.save(record);
        ConsoleHelper.printInfo("반려 처리되었습니다.");
    }

    /** 미납 알림 자동 발송 설정 */
    private static void configureOverdueNotice() {
        OverdueNoticeSetting setting = OverdueNoticeSettingDAO.find();
        if (setting == null) {
            setting = new OverdueNoticeSetting();
        }

        ConsoleHelper.printStage("재무회계 담당자", "미납 알림 자동 발송을 설정합니다.");
        ConsoleHelper.printInfo("현재 활성화 여부: " + setting.isEnabled());
        ConsoleHelper.printInfo("현재 발송 기준일: 납입일 +" + setting.getDaysAfterDue() + "일");
        if (setting.getMessageTemplate() != null) {
            ConsoleHelper.printInfo("현재 메시지 템플릿: " + setting.getMessageTemplate());
        }

        boolean enabled = ConsoleHelper.readYesNo("자동 발송을 활성화하시겠습니까?");
        setting.toggleEnabled(enabled);
        if (enabled) {
            int days = ConsoleHelper.readPositiveInt("  발송 기준일 (납입일 경과 일수): ");
            setting.setDaysAfterDue(days);
            String template = ConsoleHelper.readNonEmpty("  메시지 템플릿: ");
            setting.setMessageTemplate(template);
            ConsoleHelper.printInfo("미리보기: " + setting.previewMessage());
        }
        setting.save();
        OverdueNoticeSettingDAO.save(setting);
        ConsoleHelper.waitEnter();
    }

    private static void showRecord(PaymentRecord record) {
        ConsoleHelper.printDivider();
        System.out.println("  결제번호: " + record.getRecordNo());
        System.out.println("  대상 계약: " + record.getContract().getContractNo());
        System.out.println("  결제 일자: " + record.getPaymentDate());
        System.out.println("  결제 금액: " + record.getAmount() + "원");
        System.out.println("  결제 수단: " + record.getMethod());
        System.out.println("  상태: " + record.getStatus());
        ConsoleHelper.printDivider();
    }
}
