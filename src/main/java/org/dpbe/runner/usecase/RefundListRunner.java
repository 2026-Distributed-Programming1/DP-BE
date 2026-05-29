package org.dpbe.runner.usecase;

import java.util.List;
import org.dpbe.contract.Cancellation;
import org.dpbe.dao.CancellationDAO;
import org.dpbe.dao.RefundCalculationDAO;
import org.dpbe.dao.RefundPaymentDAO;
import org.dpbe.enums.RefundPaymentStatus;
import org.dpbe.payment.RefundCalculation;
import org.dpbe.payment.RefundPayment;
import org.dpbe.runner.ConsoleHelper;

/**
 * 해약 환급 내역을 조회한다 — UC9·UC10의 부모 유스케이스 진행자
 *
 * 진행 상태에 따라 UC9(산출)과 UC10(지급)을 맥락 안에서 분기 호출한다.
 * - 산출 대기  → UC9 RefundCalculationRunner 호출 후 복귀
 * - 산출 완료  → UC10 RefundPaymentRunner 호출 후 복귀
 * - 지급 완료  → 읽기 전용 조회만 가능
 */
public class RefundListRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("해약 환급 내역을 조회한다 (UC9·UC10 포함)");
        ConsoleHelper.printDoubleDivider();

        while (true) {
            List<Cancellation> cancellations = CancellationDAO.findAll();
            if (cancellations.isEmpty()) {
                ConsoleHelper.printError("등록된 해지 건이 없습니다.");
                ConsoleHelper.waitEnter();
                return;
            }

            // 목록 출력
            ConsoleHelper.printDivider();
            System.out.println("  [해약 환급 내역 목록]");
            for (int i = 0; i < cancellations.size(); i++) {
                Cancellation c = cancellations.get(i);
                System.out.printf("  %d. %s | 계약 %s | 고객 %s | 상태: %s%n",
                        i + 1,
                        c.getCancellationNo(),
                        c.getContract().getContractNo(),
                        c.getContract().getCustomer().getName(),
                        refundStatus(c));
            }
            ConsoleHelper.printDivider();

            String[] options = new String[cancellations.size() + 1];
            for (int i = 0; i < cancellations.size(); i++) {
                Cancellation c = cancellations.get(i);
                options[i] = c.getCancellationNo() + " (" + refundStatus(c) + ")";
            }
            options[cancellations.size()] = "목록 종료";

            int choice = ConsoleHelper.readMenuChoice("[재무회계 담당자] 상세 조회할 건을 선택하세요:", options);
            if (choice == cancellations.size() + 1) return;

            Cancellation selected = cancellations.get(choice - 1);
            showDetail(selected);

            String status = refundStatus(selected);
            if ("산출 대기".equals(status)) {
                if (ConsoleHelper.readYesNo("[재무회계 담당자] 환급금을 산출하시겠습니까? (UC9)")) {
                    RefundCalculationRunner.run(selected);
                }
            } else if ("산출 완료".equals(status)) {
                RefundPayment payment = findWaitingPayment(selected);
                if (payment != null && ConsoleHelper.readYesNo("[재무회계 담당자] 환급금을 지급하시겠습니까? (UC10)")) {
                    RefundPaymentRunner.run(payment);
                }
            } else {
                ConsoleHelper.printInfo("이미 지급이 완료된 건입니다.");
                ConsoleHelper.waitEnter();
            }
        }
    }

    private static String refundStatus(Cancellation cancellation) {
        String cancNo = cancellation.getCancellationNo();
        RefundCalculation refund = RefundCalculationDAO.findAll().stream()
                .filter(r -> r.getCancellation() != null
                        && cancNo.equals(r.getCancellation().getCancellationNo()))
                .findFirst().orElse(null);
        if (refund == null) return "산출 대기";

        String refundNo = refund.getRefundNo();
        RefundPayment payment = RefundPaymentDAO.findAll().stream()
                .filter(p -> p.getRefund() != null
                        && refundNo.equals(p.getRefund().getRefundNo()))
                .findFirst().orElse(null);
        if (payment == null) return "산출 완료";
        return payment.getStatus() == RefundPaymentStatus.COMPLETED ? "지급 완료" : "산출 완료";
    }

    private static RefundPayment findWaitingPayment(Cancellation cancellation) {
        String cancNo = cancellation.getCancellationNo();
        return RefundPaymentDAO.findAll().stream()
                .filter(p -> p.getRefund() != null
                        && p.getRefund().getCancellation() != null
                        && cancNo.equals(p.getRefund().getCancellation().getCancellationNo())
                        && p.getStatus() == RefundPaymentStatus.WAITING)
                .findFirst().orElse(null);
    }

    private static void showDetail(Cancellation cancellation) {
        ConsoleHelper.printDivider();
        System.out.println("  해지번호  : " + cancellation.getCancellationNo());
        System.out.println("  계약번호  : " + cancellation.getContract().getContractNo());
        System.out.println("  고객명    : " + cancellation.getContract().getCustomer().getName());
        System.out.println("  진행 상태 : " + refundStatus(cancellation));
        System.out.println("  해지 사유 : " + (cancellation.getReason() != null ? cancellation.getReason() : "-"));
        System.out.println("  해지 일시 : " + (cancellation.getCanceledAt() != null ? cancellation.getCanceledAt() : "-"));

        String cancNo = cancellation.getCancellationNo();
        RefundCalculation refund = RefundCalculationDAO.findAll().stream()
                .filter(r -> r.getCancellation() != null
                        && cancNo.equals(r.getCancellation().getCancellationNo()))
                .findFirst().orElse(null);
        if (refund != null) {
            System.out.println("  기본 환급금    : " + refund.getBaseRefund() + "원");
            System.out.println("  실지급 환급금  : " + refund.getFinalRefund() + "원");
        } else {
            System.out.println("  (아직 산출되지 않았습니다)");
        }
        ConsoleHelper.printDivider();
    }
}