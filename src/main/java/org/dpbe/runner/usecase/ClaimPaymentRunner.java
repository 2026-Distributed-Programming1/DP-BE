package org.dpbe.runner.usecase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.claim.ClaimPayment;
import org.dpbe.dao.ClaimPaymentDAO;
import org.dpbe.enums.ClaimPaymentStatus;
import org.dpbe.enums.NoticeMethod;
import org.dpbe.enums.PaymentType;
import org.dpbe.runner.ConsoleHelper;

/**
 * UC6: 「보험금을 지급한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 지급 건 선택 → 지급 유형 선택 → OTP 인증 → 이체 실행 → 안내 발송 → 종결
 * 분기:
 * - A1: 예약 지급
 * - A2: 이전 페이지로 이동
 * - E1: OTP 인증 실패
 * - E2: 이체 실패 → 계좌 변경 안내
 */
public class ClaimPaymentRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC6: 보험금을 지급한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 지급 건 선택
        ClaimPayment payment = selectPayment();
        if (payment == null) return;

        showPayment(payment);

        // 2) 지급 유형 선택
        int typeChoice = ConsoleHelper.readMenuChoice(
                "[보상담당자] 지급 유형을 선택하세요:",
                "즉시 지급 (정상)",
                "예약 지급 (A1)",
                "이전 페이지 (A2)");
        if (typeChoice == 3) {
            return;
        }

        if (typeChoice == 1) {
            payment.selectPaymentType(PaymentType.IMMEDIATE);
        } else {
            payment.selectPaymentType(PaymentType.SCHEDULED);
            LocalDateTime when = ConsoleHelper.readDateTime("  예약 일시");
            payment.setScheduledDateTime(when);
        }

        // 3) 안내 메시지 옵션 선택
        List<Integer> noticeIdx = ConsoleHelper.readMultiChoice(
                "[보상담당자] 안내 메시지 옵션을 선택하세요 (다중 선택 가능):",
                "알림톡", "문자");
        List<NoticeMethod> noticeOptions = new ArrayList<>();
        for (int i : noticeIdx) {
            noticeOptions.add(i == 1 ? NoticeMethod.KAKAO : NoticeMethod.SMS);
        }
        payment.setNoticeOption(noticeOptions);

        // 4) OTP 인증 (E1)
        if (!handleOTP(payment)) {
            ConsoleHelper.printError("OTP 인증 실패로 지급을 진행할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 5) 이체 실행 (예약 vs 즉시)
        if (payment.getPaymentType() == PaymentType.SCHEDULED) {
            payment.schedule();
        } else {
            // E2 시뮬레이션 옵션
            boolean simulateFail = ConsoleHelper.readYesNo(
                    "  [E2 시뮬레이션] 이체 실패 상황을 시뮬레이션하시겠습니까?");
            if (simulateFail) {
                payment.handleTransferFailure("계좌 정보 오류");
                ClaimPaymentDAO.save(payment);
                payment.sendAccountChangeNotice();
                ConsoleHelper.printError("[E2] 이체에 실패했습니다. 고객에게 계좌 변경 안내를 발송했습니다.");
                ConsoleHelper.waitEnter();
                return;
            }
            payment.execute();
        }

        // 6) 안내 메시지 발송 + 종결
        if (payment.getStatus() == ClaimPaymentStatus.COMPLETED) {
            payment.sendCompletionNotice();
            payment.close();
            ConsoleHelper.printStage("시스템", "지급 완료 안내를 출력합니다.");
            if (payment.getRecipient() != null) ConsoleHelper.printInfo("수령인: " + payment.getRecipient().getName());
            if (payment.getAccount() != null) ConsoleHelper.printInfo("은행명: " + payment.getAccount().getBankName()
                    + " | 계좌번호: " + payment.getAccount().getAccountNo());
            ConsoleHelper.printInfo("이체금액: " + payment.getFinalAmount() + "원");
        } else if (payment.getStatus() == ClaimPaymentStatus.SCHEDULED) {
            ConsoleHelper.printInfo("예약 지급이 등록되었습니다. 예약 시점에 자동으로 이체됩니다.");
        }
        ClaimPaymentDAO.save(payment);

        ConsoleHelper.waitEnter();
    }

    /** UC5에서 이관받아 실행 — 지급 건 선택 없이 직접 처리 (UC5 → UC6 체인) */
    static void run(ClaimPayment payment) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC6: 보험금을 지급한다");
        ConsoleHelper.printDoubleDivider();

        showPayment(payment);

        int typeChoice = ConsoleHelper.readMenuChoice(
                "[보상담당자] 지급 유형을 선택하세요:",
                "즉시 지급 (정상)",
                "예약 지급 (A1)",
                "이전 페이지 (A2)");
        if (typeChoice == 3) {
            return;
        }

        if (typeChoice == 1) {
            payment.selectPaymentType(PaymentType.IMMEDIATE);
        } else {
            payment.selectPaymentType(PaymentType.SCHEDULED);
            LocalDateTime when = ConsoleHelper.readDateTime("  예약 일시");
            payment.setScheduledDateTime(when);
        }

        List<Integer> noticeIdx = ConsoleHelper.readMultiChoice(
                "[보상담당자] 안내 메시지 옵션을 선택하세요 (다중 선택 가능):",
                "알림톡", "문자");
        List<NoticeMethod> noticeOptions = new ArrayList<>();
        for (int i : noticeIdx) {
            noticeOptions.add(i == 1 ? NoticeMethod.KAKAO : NoticeMethod.SMS);
        }
        payment.setNoticeOption(noticeOptions);

        if (!handleOTP(payment)) {
            ConsoleHelper.printError("OTP 인증 실패로 지급을 진행할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        if (payment.getPaymentType() == PaymentType.SCHEDULED) {
            payment.schedule();
        } else {
            boolean simulateFail = ConsoleHelper.readYesNo(
                    "  [E2 시뮬레이션] 이체 실패 상황을 시뮬레이션하시겠습니까?");
            if (simulateFail) {
                payment.handleTransferFailure("계좌 정보 오류");
                ClaimPaymentDAO.save(payment);
                payment.sendAccountChangeNotice();
                ConsoleHelper.printError("[E2] 이체에 실패했습니다. 고객에게 계좌 변경 안내를 발송했습니다.");
                ConsoleHelper.waitEnter();
                return;
            }
            payment.execute();
        }

        if (payment.getStatus() == ClaimPaymentStatus.COMPLETED) {
            payment.sendCompletionNotice();
            payment.close();
            ConsoleHelper.printStage("시스템", "지급 완료 안내를 출력합니다.");
            if (payment.getRecipient() != null) ConsoleHelper.printInfo("수령인: " + payment.getRecipient().getName());
            if (payment.getAccount() != null) ConsoleHelper.printInfo("은행명: " + payment.getAccount().getBankName()
                    + " | 계좌번호: " + payment.getAccount().getAccountNo());
            ConsoleHelper.printInfo("이체금액: " + payment.getFinalAmount() + "원");
        } else if (payment.getStatus() == ClaimPaymentStatus.SCHEDULED) {
            ConsoleHelper.printInfo("예약 지급이 등록되었습니다. 예약 시점에 자동으로 이체됩니다.");
        }
        ClaimPaymentDAO.save(payment);

        ConsoleHelper.waitEnter();
    }

    /** OTP 인증 처리 (E1: 인증 실패 시 재시도 허용) */
    private static boolean handleOTP(ClaimPayment payment) {
        int attempt = 0;
        while (attempt < 5) {
            attempt++;
            String otp = ConsoleHelper.readNonEmpty("[보상담당자] OTP 6자리 입력 (시도 " + attempt + "/5): ");
            payment.enterOTP(otp);
            if (payment.verifyOTP()) {
                ConsoleHelper.printSuccess("OTP 인증 완료");
                return true;
            }
            ConsoleHelper.printError("[E1] OTP 인증에 실패했습니다.");
        }
        return false;
    }

    private static void showPayment(ClaimPayment payment) {
        ConsoleHelper.printDivider();
        System.out.println("  지급번호: " + payment.getPaymentNo());
        System.out.println("  산출번호: " + payment.getCalculation().getCalculationNo());
        // 접수번호·사고일자: 산출 → 조사 → 청구 체인 탐색
        org.dpbe.claim.ClaimCalculation calc = payment.getCalculation();
        if (calc.getInvestigation() != null && calc.getInvestigation().getClaim() != null) {
            System.out.println("  접수번호: " + calc.getInvestigation().getClaim().getClaimNo());
        }
        if (calc.getInvestigation() != null
                && calc.getInvestigation().getClaim() != null
                && calc.getInvestigation().getClaim().getContract() != null) {
            System.out.println("  사고일자: " + calc.getInvestigation().getClaim().getContract().getContractDate());
        }
        if (payment.getRecipient() != null) {
            System.out.println("  수령인: " + payment.getRecipient().getName()
                    + " (" + payment.getRecipient().getContact() + ")");
        }
        if (payment.getAccount() != null) {
            System.out.println("  수령 계좌: " + payment.getAccount().getBankName()
                    + " " + payment.getAccount().getAccountNo());
        }
        System.out.println("  지급액: " + payment.getFinalAmount() + "원");
        ConsoleHelper.printDivider();
    }

    private static ClaimPayment selectPayment() {
        List<ClaimPayment> available = ClaimPaymentDAO.findAll().stream()
                .filter(p -> p.getStatus() == ClaimPaymentStatus.WAITING)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            ConsoleHelper.printError("처리할 지급 건이 없습니다. 먼저 보험금 산출을 완료해주세요.");
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
