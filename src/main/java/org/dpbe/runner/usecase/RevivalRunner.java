package org.dpbe.runner.usecase;

import org.dpbe.actor.Customer;
import org.dpbe.consultation.Revival;
import org.dpbe.contract.Contract;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.RevivalDAO;
import org.dpbe.runner.ConsoleHelper;
import java.util.List;

/**
 * UC: 부활을 요청한다 시나리오 진행자
 * (보험상품을 조회한다 A1에서 이동)
 *
 * Basic Path:
 *   1. 고객은 실효된 보험상품 상세 화면에서 [부활 신청] 버튼을 클릭한다.
 *   2. 시스템은 부활 신청 화면을 출력한다. (부활 가능 여부 확인) (E1)
 *   3. 고객은 부활 신청 정보(연락처, 납입방법)를 입력한다.
 *   4. 시스템은 미납보험료 및 이자 산출 결과를 출력한다.
 *   5. 고객은 납입 방법을 선택하고 [납입] 버튼을 클릭한다.
 *   6. 시스템은 납입 처리 결과를 출력한다. (E2)
 *   7. 고객은 부활 신청 내용을 확인 후 [부활 신청] 버튼을 클릭한다.
 *   8. 시스템은 본인인증 화면을 출력한다.
 *   9. 고객은 인증 방법을 선택하고 본인인증을 완료한다.
 *  10. 시스템은 부활 신청 완료 결과를 출력하고, 보험 심사자에게 부활 신청 내역을 자동 전달한다.
 *
 * Alternative:
 *   A1) 부활 가능 기간이 초과된 경우 → 안내 후 보험상품을 조회한다 유스케이스로 이동
 *
 * Exception:
 *   E1) 부활 불가 상품인 경우 → 안내 메시지 출력
 *   E2) 납입 처리 실패 시 → 재시도 안내
 */
public class RevivalRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 부활을 요청한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        Revival revival = new Revival();
        revival.setCustomer(customer);

        // 1-1. 고객 계약 목록 조회 후 선택
        List<Contract> contracts = ContractDAO.findByCustomerId(customer.getCustomerId());
        if (contracts.isEmpty()) {
            ConsoleHelper.printError("해당 고객의 계약이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        String[] contractOptions = contracts.stream()
                .map(c -> c.getContractNo() + " - " + c.getPolicyNo())
                .toArray(String[]::new);
        int contractChoice = ConsoleHelper.readMenuChoice("[시스템] 부활 신청할 계약을 선택하세요:", contractOptions);
        revival.setContractNo(contracts.get(contractChoice - 1).getContractNo());

        // 2. 시스템은 부활 신청 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "부활 신청 화면을 출력합니다.");
        ConsoleHelper.printInfo("실효사유 / 실효일자 / 미납보험료 / 부활가능여부");

        // 부활 가능 여부 확인 (E1)
        boolean eligible = revival.checkEligibility();
        if (!eligible) {
            // E1) 부활 불가 상품인 경우
            ConsoleHelper.printError("[E1] 해당 상품은 부활 신청이 불가합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // A1) 부활 가능 기간 초과 여부 확인
        boolean expired = ConsoleHelper.readYesNo("[시스템] 부활 가능 기간이 초과되었습니까? (테스트용)");
        if (expired) {
            ConsoleHelper.printError("[A1] 부활 가능 기간이 초과되었습니다. 신규 가입을 진행해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 3. 고객은 부활 신청 정보를 입력한다.
        ConsoleHelper.printStage("고객", "부활 신청 정보를 입력합니다.");
        String contact = ConsoleHelper.readNonEmpty("  연락처: ");
        revival.setContact(contact);

        // 4. 시스템은 미납보험료 및 이자 산출 결과를 출력한다.
        revival.setUnpaidAmount(150000L);
        long unpaid = revival.calculateUnpaidAmount();
        ConsoleHelper.printStage("시스템", "미납보험료 및 이자 산출 결과를 출력합니다.");
        ConsoleHelper.printInfo("미납금액: " + unpaid + "원 | 이자금액: 5000원 | 총납입금액: " + (unpaid + 5000) + "원");

        // 5. 고객은 납입 방법을 선택하고 [납입] 버튼을 클릭한다.
        String paymentMethod = ConsoleHelper.readNonEmpty("  납입방법 (카드/계좌이체): ");
        boolean paySuccess = revival.pay(paymentMethod);

        // 6. 시스템은 납입 처리 결과를 출력한다. (E2)
        if (!paySuccess) {
            ConsoleHelper.printError("[E2] 납입 처리에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printStage("시스템", "납입 처리 결과를 출력합니다.");
        ConsoleHelper.printInfo("납입금액: " + (unpaid + 5000) + "원 | 영수증번호: RCP-001");

        // 7. 고객은 부활 신청 내용을 확인 후 [부활 신청] 버튼을 클릭한다.
        boolean submit = ConsoleHelper.readYesNo("[고객] 부활을 신청하시겠습니까?");
        if (!submit) {
            ConsoleHelper.waitEnter();
            return;
        }

        // 8~9. 시스템은 본인인증 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "본인인증 화면을 출력합니다. (휴대폰 인증 / 공동인증서)");
        boolean authenticated = revival.authenticate();
        if (!authenticated) {
            ConsoleHelper.printError("[E] 본인인증에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 10. 시스템은 부활 신청 완료 결과를 출력한다.
        revival.submit();
        RevivalDAO.save(revival);
        ConsoleHelper.printStage("시스템", "부활 신청 완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("신청번호: " + revival.getRevivalNumber()
                + " | 신청일시: " + revival.getAppliedAt());
        ConsoleHelper.printStage("시스템", "보험 심사자에게 부활 신청 내역을 자동 전달합니다.");

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
        int choice = ConsoleHelper.readMenuChoice("[시스템] 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }
}
