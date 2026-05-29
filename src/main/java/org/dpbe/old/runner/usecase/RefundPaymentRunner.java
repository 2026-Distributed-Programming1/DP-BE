package org.dpbe.old.runner.usecase;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.old.dao.RefundPaymentDAO;
import org.dpbe.domain.common.enums.RefundPaymentStatus;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC10: 「해약 환급금을 지급한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 지급 건 선택 → OTP 인증 → 이체 실행 → 알림톡 발송
 * 분기:
 * - A1: 목록으로 돌아가기
 * - E1: OTP 5회 실패 → 잠금
 * - E2: 이체 실패
 * - E3: 알림톡 발송 실패
 */
public class RefundPaymentRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC10: 해약 환급금을 지급한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 지급 건 선택
        RefundPayment payment = selectPayment();
        if (payment == null) return;

        // 계좌가 자동 로드되지 않은 경우 수동 주입 시도
        if (payment.getAccount() == null) {
            ConsoleHelper.printWarning("수령 계좌가 로드되지 않았습니다. 고객 등록 계좌를 사용합니다.");
            // 시연 단순화: 고객의 첫 등록 계좌 사용
            if (payment.getRefund() != null
                    && payment.getRefund().getCancellation() != null
                    && payment.getRefund().getCancellation().getContract() != null) {
                List<BankAccount> accounts = payment.getRefund().getCancellation().getContract()
                        .getCustomer().getRegisteredAccounts();
                if (!accounts.isEmpty()) {
                    payment.setAccount(accounts.get(0));
                    ConsoleHelper.printInfo("계좌가 자동으로 설정되었습니다: "
                            + accounts.get(0).getBankName() + " " + accounts.get(0).getAccountNo());
                }
            }
        }

        showPayment(payment);

        // 2) 작업 선택
        int choice = ConsoleHelper.readMenuChoice("[재무회계 담당자] 다음 작업을 선택하세요:",
                "OTP 인증 후 이체 실행 (정상)",
                "목록으로 돌아가기 (A1)");
        if (choice == 2) {
            return;
        }

        // 3) OTP 인증 (E1: 5회 실패 시 잠금)
        if (!handleOTP(payment)) {
            return;
        }

        // 4) E2 시뮬레이션 옵션
        boolean simulateFail = ConsoleHelper.readYesNo(
                "  [E2 시뮬레이션] 이체 실패 상황을 시뮬레이션하시겠습니까?");
        if (simulateFail) {
            // 검증되지 않은 계좌로 강제 변경하여 실패 유도
            BankAccount fakeAccount = new BankAccount();
            payment.setAccount(fakeAccount);
        }

        // 5) 이체 실행
        payment.execute();

        if (payment.getStatus() == RefundPaymentStatus.FAILED) {
            RefundPaymentDAO.save(payment);
            ConsoleHelper.printError("[E2] 이체 처리에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 지급 완료 팝업
        ConsoleHelper.printStage("시스템", "지급 완료 안내를 출력합니다.");
        if (payment.getAccount() != null) {
            ConsoleHelper.printInfo("수령인명: " + payment.getAccount().getAccountHolder());
            ConsoleHelper.printInfo("은행명: " + payment.getAccount().getBankName()
                    + " | 계좌번호: " + payment.getAccount().getAccountNo());
        }
        ConsoleHelper.printInfo("이체금액: " + payment.getFinalAmount() + "원");

        // 6) 알림톡 발송 (E3 시뮬레이션 옵션)
        boolean noticeFail = ConsoleHelper.readYesNo(
                "  [E3 시뮬레이션] 알림톡 발송 실패 상황을 시뮬레이션하시겠습니까?");
        if (noticeFail) {
            payment.handleNoticeFailure();
            ConsoleHelper.printError("[E3] 알림톡 발송에 실패했습니다. (이체는 이미 완료됨)");
        } else {
            payment.sendNotice();
        }
        RefundPaymentDAO.save(payment);

        ConsoleHelper.waitEnter();
    }

    /** RefundListRunner에서 이관받아 실행 — 지급 건 선택 없이 직접 처리 */
    static void run(RefundPayment payment) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC10: 해약 환급금을 지급한다");
        ConsoleHelper.printDoubleDivider();

        if (payment.getAccount() == null) {
            ConsoleHelper.printWarning("수령 계좌가 로드되지 않았습니다. 고객 등록 계좌를 사용합니다.");
            if (payment.getRefund() != null
                    && payment.getRefund().getCancellation() != null
                    && payment.getRefund().getCancellation().getContract() != null) {
                List<BankAccount> accounts = payment.getRefund().getCancellation().getContract()
                        .getCustomer().getRegisteredAccounts();
                if (!accounts.isEmpty()) {
                    payment.setAccount(accounts.get(0));
                    ConsoleHelper.printInfo("계좌가 자동으로 설정되었습니다: "
                            + accounts.get(0).getBankName() + " " + accounts.get(0).getAccountNo());
                }
            }
        }

        showPayment(payment);

        int choice = ConsoleHelper.readMenuChoice("[재무회계 담당자] 다음 작업을 선택하세요:",
                "OTP 인증 후 이체 실행 (정상)",
                "목록으로 돌아가기 (A1)");
        if (choice == 2) {
            return;
        }

        if (!handleOTP(payment)) {
            return;
        }

        boolean simulateFail = ConsoleHelper.readYesNo(
                "  [E2 시뮬레이션] 이체 실패 상황을 시뮬레이션하시겠습니까?");
        if (simulateFail) {
            BankAccount fakeAccount = new BankAccount();
            payment.setAccount(fakeAccount);
        }

        payment.execute();

        if (payment.getStatus() == RefundPaymentStatus.FAILED) {
            RefundPaymentDAO.save(payment);
            ConsoleHelper.printError("[E2] 이체 처리에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 지급 완료 팝업
        ConsoleHelper.printStage("시스템", "지급 완료 안내를 출력합니다.");
        if (payment.getAccount() != null) {
            ConsoleHelper.printInfo("수령인명: " + payment.getAccount().getAccountHolder());
            ConsoleHelper.printInfo("은행명: " + payment.getAccount().getBankName()
                    + " | 계좌번호: " + payment.getAccount().getAccountNo());
        }
        ConsoleHelper.printInfo("이체금액: " + payment.getFinalAmount() + "원");

        boolean noticeFail = ConsoleHelper.readYesNo(
                "  [E3 시뮬레이션] 알림톡 발송 실패 상황을 시뮬레이션하시겠습니까?");
        if (noticeFail) {
            payment.handleNoticeFailure();
            ConsoleHelper.printError("[E3] 알림톡 발송에 실패했습니다. (이체는 이미 완료됨)");
        } else {
            payment.sendNotice();
        }
        RefundPaymentDAO.save(payment);

        ConsoleHelper.waitEnter();
    }

    /** OTP 인증 (E1: 5회 실패 시 잠금) */
    private static boolean handleOTP(RefundPayment payment) {
        while (!payment.isLocked() && !payment.isOtpVerified()) {
            int remaining = 5 - payment.getOtpFailCount();
            String otp = ConsoleHelper.readNonEmpty(
                    "[재무회계 담당자] OTP 6자리 입력 (남은 시도: " + remaining + "회): ");
            payment.enterOTP(otp);
            payment.verifyOTP();
            if (payment.isOtpVerified()) {
                ConsoleHelper.printSuccess("OTP 인증 완료");
                return true;
            }
            ConsoleHelper.printError("OTP 인증에 실패했습니다.");

            if (payment.isLocked()) {
                ConsoleHelper.printError("[E1] OTP 5회 실패로 계정이 잠겼습니다. 관리자에게 문의하세요.");
                ConsoleHelper.waitEnter();
                return false;
            }
        }
        return payment.isOtpVerified();
    }

    private static void showPayment(RefundPayment payment) {
        ConsoleHelper.printDivider();
        System.out.println("  지급번호: " + payment.getPaymentNo());
        if (payment.getRefund() != null) {
            System.out.println("  대상 산출: " + payment.getRefund().getRefundNo());
        }
        if (payment.getAccount() != null) {
            System.out.println("  수령 계좌: " + payment.getAccount().getBankName()
                    + " " + payment.getAccount().getAccountNo());
        }
        System.out.println("  지급액: " + payment.getFinalAmount() + "원");
        System.out.println("  상태: " + payment.getStatus());
        ConsoleHelper.printDivider();
    }

    private static RefundPayment selectPayment() {
        List<RefundPayment> available = RefundPaymentDAO.findAll().stream()
                .filter(p -> p.getStatus() == RefundPaymentStatus.WAITING)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            ConsoleHelper.printError("처리할 환급금 지급 건이 없습니다. 먼저 환급금 산출을 완료해주세요.");
            ConsoleHelper.waitEnter();
            return null;
        }
        String[] options = available.stream()
                .map(p -> p.getPaymentNo() + " - " + p.getFinalAmount() + "원")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 처리할 지급 건을 선택하세요:", options);
        return available.get(choice - 1);
    }
}
