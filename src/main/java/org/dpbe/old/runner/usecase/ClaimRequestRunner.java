package org.dpbe.old.runner.usecase;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.AccidentDetail;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.common.entity.Attachment;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.old.dao.ClaimRequestDAO;
import org.dpbe.old.dao.ContractDAO;
import org.dpbe.old.dao.CustomerDAO;
import org.dpbe.domain.common.enums.AccidentSubType;
import org.dpbe.domain.common.enums.AuthMethod;
import org.dpbe.domain.common.enums.ClaimType;
import org.dpbe.domain.common.enums.NoticeMethod;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC3: 「보험금을 요청한다」 시나리오 진행자
 *
 * 정상 흐름과 함께 다음 분기를 시뮬레이션한다.
 * - A2: 청구 유형이 "재해"인 경우 사고 상세 추가 입력
 * - A3: 실손 의료비 청구 확인
 * - A4: 등록된 계좌 사용 vs 새 계좌 등록
 * - E1: 본인 명의가 아닌 계좌 인증 시도 시 실패 후 재입력
 */
public class ClaimRequestRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC3: 보험금을 요청한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 고객 선택
        Customer customer = selectCustomer();
        if (customer == null) return;

        // 2) 대상 계약 선택
        Contract contract = selectContract(customer);
        if (contract == null) return;

        // 3) ClaimRequest 생성
        ClaimRequest claim = new ClaimRequest(customer, contract);

        // 4) 개인정보 동의
        if (!ConsoleHelper.readYesNo("[고객] 개인정보 수집·이용에 동의하시겠습니까?")) {
            ConsoleHelper.printError("개인정보 미동의로 청구를 진행할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        claim.agreePersonalInfoTerms();

        // 5) 본인인증
        ConsoleHelper.printStage("고객", "본인인증을 진행합니다.");
        int authChoice = ConsoleHelper.readMenuChoice("인증 방법을 선택하세요:",
                "휴대폰", "간편 인증", "공동인증서");
        AuthMethod authMethod = AuthMethod.values()[authChoice - 1];
        claim.selectAuthMethod(authMethod);
        if (!claim.authenticate()) {
            ConsoleHelper.printError("본인인증에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("본인인증 완료");

        // 6) 수령인 정보 (자동 입력)
        claim.confirmRecipientInfo();
        ConsoleHelper.printInfo("수령인 정보가 자동으로 입력되었습니다: " + claim.getRecipientInfo().getName());
        if (ConsoleHelper.readYesNo("  수령인 연락처를 변경하시겠습니까?")) {
            String newContact = ConsoleHelper.readNonEmpty("  새 연락처: ");
            claim.changeRecipientContact(newContact);
        }

        // 7) 피보험자 선택
        boolean sameAsCustomer = ConsoleHelper.readYesNo("[고객] 피보험자가 본인입니까?");
        if (sameAsCustomer) {
            claim.selectInsured(customer);
        } else {
            String insuredName = ConsoleHelper.readNonEmpty("  피보험자 이름: ");
            String relationship = ConsoleHelper.readNonEmpty("  관계 (예: 배우자, 자녀): ");
            ConsoleHelper.printInfo("피보험자: " + insuredName + " (" + relationship + ")");
            claim.selectInsured(customer); // 시스템에 피보험자 별도 등록 구조가 없으므로 연결 유지
        }

        // 8) 청구 유형 선택 (A2 분기점)
        int typeChoice = ConsoleHelper.readMenuChoice("[고객] 청구 유형을 선택하세요:",
                "질병", "재해");
        ClaimType claimType = (typeChoice == 1) ? ClaimType.DISEASE : ClaimType.ACCIDENT;
        claim.selectClaimType(claimType);

        // A2: 재해인 경우 사고 상세 입력
        if (claimType == ClaimType.ACCIDENT) {
            ConsoleHelper.printInfo("[A2] 재해 청구이므로 사고 상세 정보를 추가로 입력합니다.");
            AccidentDetail detail = new AccidentDetail();
            int subChoice = ConsoleHelper.readMenuChoice("사고 유형을 선택하세요:",
                    "일반재해", "교통재해");
            AccidentSubType subType = (subChoice == 1) ? AccidentSubType.GENERAL : AccidentSubType.TRAFFIC;
            String content = ConsoleHelper.readNonEmpty("  사고 내용: ");
            LocalDate date = ConsoleHelper.readDate("  사고 날짜");
            String location = ConsoleHelper.readNonEmpty("  사고 장소: ");
            detail.enter(subType, content, date, location);
            claim.enterAccidentDetail(detail);
            ConsoleHelper.printSuccess("사고 상세 정보 입력 완료");
        }

        // 9) 청구 사유 (다중 선택, A3 분기점)
        List<Integer> reasonIdx = ConsoleHelper.readMultiChoice(
                "[고객] 청구 사유를 선택하세요 (다중 선택 가능):",
                "입원", "수술", "통원", "실손 의료비");
        String[] reasonNames = {"입원", "수술", "통원", "실손"};
        List<String> reasons = reasonIdx.stream()
                .map(i -> reasonNames[i - 1])
                .collect(Collectors.toList());
        claim.selectClaimReasons(reasons);

        // A3: 실손이 포함된 경우
        if (reasons.contains("실손")) {
            ConsoleHelper.printInfo("[A3] 실손 의료비가 선택되어 별도 확인을 진행합니다.");
            claim.confirmInsuranceBenefits();
        }

        // 10) 진단명 입력
        String diagnosis = ConsoleHelper.readNonEmpty("[고객] 병명/진단명을 입력하세요: ");
        claim.enterDiagnosis(diagnosis);

        // 11) 계좌 선택 (A4 분기점, E1 예외 처리)
        if (!handleAccount(claim, customer)) {
            return;
        }

        // 12) 안내 방법
        int noticeChoice = ConsoleHelper.readMenuChoice(
                "[고객] 진행과정 안내 방법을 선택하세요:",
                "알림톡", "문자", "이메일", "우편", "신청 안 함");
        claim.selectNoticeMethod(NoticeMethod.values()[noticeChoice - 1]);
        claim.setProgressNoticeAgreed(noticeChoice != 5);
        claim.setFpNoticeAgreed(ConsoleHelper.readYesNo("  담당 FP에게도 통지를 받으시겠습니까?"));

        // 13) 첨부 서류 (시연용)
        if (ConsoleHelper.readYesNo("[고객] 진단서/영수증 등 서류를 첨부하시겠습니까?")) {
            claim.attachDocument(new Attachment(new File("진단서.pdf")));
            claim.attachDocument(new Attachment(new File("영수증.pdf")));
            ConsoleHelper.printSuccess("서류 2건 첨부 완료");
        }

        // 14) 최종 검증 후 제출
        if (!claim.validateBeforeSubmit()) {
            ConsoleHelper.printError("필수 입력값이 누락되어 제출할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        claim.submit();
        ClaimRequestDAO.save(claim);
        ConsoleHelper.printSuccess("청구번호: " + claim.getClaimNo());

        ConsoleHelper.waitEnter();
    }

    /** A4 + E1 처리: 계좌 선택 및 인증 */
    private static boolean handleAccount(ClaimRequest claim, Customer customer) {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice("[고객] 보험금 수령 계좌를 어떻게 처리하시겠습니까?",
                    "등록된 계좌 사용 (A4)",
                    "새 계좌 등록");
            if (choice == 1) {
                List<BankAccount> accounts = customer.getRegisteredAccounts();
                if (accounts.isEmpty()) {
                    ConsoleHelper.printWarning("등록된 계좌가 없습니다. 새 계좌를 등록해주세요.");
                    continue;
                }
                String[] options = accounts.stream()
                        .map(a -> a.getBankName() + " " + a.getAccountNo() + " (" + a.getAccountHolder() + ")")
                        .toArray(String[]::new);
                int idx = ConsoleHelper.readMenuChoice("등록된 계좌 중 선택:", options);
                claim.selectExistingAccount(accounts.get(idx - 1));
                if (claim.verifyAccount()) {
                    ConsoleHelper.printSuccess("계좌 인증 완료");
                    return true;
                }
            } else {
                String bank = ConsoleHelper.readNonEmpty("  은행명: ");
                String accNo = ConsoleHelper.readNonEmpty("  계좌번호: ");
                claim.registerNewAccount(bank, accNo);
                if (claim.verifyAccount()) {
                    ConsoleHelper.printSuccess("계좌 인증 완료");
                    return true;
                } else {
                    // E1: 본인 명의 계좌가 아님
                    ConsoleHelper.printError("[E1] 본인 명의 계좌가 아니거나 정보가 잘못되었습니다.");
                    if (!ConsoleHelper.readYesNo("  계좌 정보를 다시 입력하시겠습니까?")) {
                        ConsoleHelper.printInfo("청구를 중단합니다.");
                        ConsoleHelper.waitEnter();
                        return false;
                    }
                }
            }
        }
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
        int choice = ConsoleHelper.readMenuChoice("[시스템] 청구할 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }

    private static Contract selectContract(Customer customer) {
        List<Contract> contracts = ContractDAO.findByCustomerId(customer.getCustomerId());
        if (contracts.isEmpty()) {
            ConsoleHelper.printError("해당 고객의 보험 계약이 없습니다.");
            ConsoleHelper.waitEnter();
            return null;
        }
        String[] options = contracts.stream()
                .map(c -> c.getContractNo() + " (월 " + c.getMonthlyPremium() + "원, "
                        + c.getContractDate() + " ~ " + c.getExpiryDate() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[고객] 청구할 계약을 선택하세요:", options);
        return contracts.get(choice - 1);
    }
}
