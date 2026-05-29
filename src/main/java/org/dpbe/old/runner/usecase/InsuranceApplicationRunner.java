package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.entity.InsuranceApplication;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.old.dao.CustomerDAO;
import org.dpbe.old.dao.InsuranceApplicationDAO;
import org.dpbe.old.dao.InsuranceProductDAO;
import org.dpbe.old.runner.ConsoleHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * UC: 보험을 신청한다 시나리오 진행자
 * (보험상품을 조회한다 A2에서 이동)
 *
 * Basic Path:
 *   1. 고객은 상품 상세 화면에서 [보험 신청] 버튼을 클릭한다.
 *   2. 시스템은 보험 신청 화면을 출력한다.
 *   3. 고객은 개인정보(이름, 생년월일, 연락처, 주소)를 입력한다.
 *   4. 시스템은 보험료 산출 결과를 출력한다.
 *   5. 고객은 특약 항목을 선택하고 납입방법을 선택한다. (A1)
 *   6. 시스템은 최종 보험료 및 보장내용 요약을 출력한다.
 *   7. 고객은 내용을 확인 후 [신청] 버튼을 클릭한다.
 *   8. 시스템은 본인인증 화면을 출력한다.
 *   9. 고객은 인증 방법을 선택하고 본인인증을 완료한다.
 *  10. 시스템은 신청 완료 결과를 출력하고, 보험 심사자에게 신청 내역을 자동 전달한다.
 *
 * Alternative:
 *   A1) 고객이 특약을 추가하는 경우 → 특약 추가 후 변경된 보험료 출력
 *
 * Exception:
 *   E1) 본인인증 실패 시 → 재시도 안내
 *   E2) 가입 불가 고객인 경우 → 고객센터 문의 안내
 */
public class InsuranceApplicationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 보험을 신청한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        // 2. 시스템은 보험 신청 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "보험 신청 화면을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 개인정보 / 보장내용 / 특약 선택 / 납입방법");

        List<InsuranceProduct> products = InsuranceProductDAO.findAll();
        if (products.isEmpty()) {
            ConsoleHelper.printError("등록된 보험상품이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        for (int i = 0; i < products.size(); i++) {
            ConsoleHelper.printInfo("[" + (i + 1) + "] " + products.get(i).getProductName()
                    + " | 월 " + products.get(i).getMonthlyPremium() + "원");
        }
        int productChoice = ConsoleHelper.readMenuChoice(
                "[고객] 신청할 상품을 선택하세요:",
                products.stream().map(InsuranceProduct::getProductName).toArray(String[]::new));
        InsuranceProduct selectedProduct = products.get(productChoice - 1);

        InsuranceApplication application = new InsuranceApplication();
        application.setCustomer(customer);
        application.setProduct(selectedProduct);

        // 3. 고객은 개인정보를 입력한다.
        ConsoleHelper.printStage("고객", "개인정보를 입력합니다.");
        String name = ConsoleHelper.readNonEmpty("  이름: ");
        String birthDate = ConsoleHelper.readNonEmpty("  생년월일 (yyyy-MM-dd): ");
        String contact = ConsoleHelper.readNonEmpty("  연락처: ");
        String address = ConsoleHelper.readNonEmpty("  주소: ");
        application.enterPersonalInfo(name, birthDate, contact, address);

        // 4. 시스템은 보험료 산출 결과를 출력한다.
        ConsoleHelper.printStage("시스템", "보험료 산출 결과를 출력합니다.");
        ConsoleHelper.printInfo("기본보험료 / 특약보험료 / 총보험료");

        // 5. 고객은 특약 항목을 선택한다. (A1)
        ConsoleHelper.printStage("고객", "특약을 선택합니다.");
        String specialTermsInput = ConsoleHelper.readLine("  특약 (쉼표로 구분, 없으면 엔터): ");
        List<String> specialTerms = new ArrayList<>();
        if (!specialTermsInput.isEmpty()) {
            for (String term : specialTermsInput.split(",")) {
                specialTerms.add(term.trim());
            }
            // A1) 특약 추가 후 변경된 보험료 출력
            ConsoleHelper.printStage("시스템", "[A1] 특약 추가 후 변경된 보험료를 출력합니다.");
        }
        application.selectSpecialTerms(specialTerms);

        String paymentMethod = ConsoleHelper.readNonEmpty("  납입방법 (월납/연납): ");
        application.selectPaymentMethod(paymentMethod);

        // 6. 시스템은 최종 보험료 및 보장내용 요약을 출력한다.
        ConsoleHelper.printStage("시스템", "최종 보험료 및 보장내용 요약을 출력합니다.");
        ConsoleHelper.printInfo("납입방법: " + application.getPaymentMethod()
                + " | 선택특약: " + application.getSelectedSpecialTerms());

        // 7. 고객은 [신청] 버튼을 클릭한다.
        boolean apply = ConsoleHelper.readYesNo("[고객] 내용을 확인 후 신청하시겠습니까?");
        if (!apply) {
            ConsoleHelper.waitEnter();
            return;
        }

        // 8~9. 시스템은 본인인증 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "본인인증 화면을 출력합니다. (휴대폰 인증 / 공동인증서)");
        boolean authenticated = application.authenticate();
        if (!authenticated) {
            // E1) 본인인증 실패 시
            ConsoleHelper.printError("[E1] 본인인증에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 10. 시스템은 신청 완료 결과를 출력한다.
        application.apply();
        InsuranceApplicationDAO.save(application);
        ConsoleHelper.printStage("시스템", "신청 완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("신청번호: " + application.getApplicationNumber()
                + " | 신청일시: " + application.getAppliedAt());
        ConsoleHelper.printStage("시스템", "보험 심사자에게 신청 내역을 자동 전달합니다.");

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
