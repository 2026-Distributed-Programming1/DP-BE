package org.dpbe.runner.usecase;

import java.time.LocalDate;
import org.dpbe.actor.Customer;
import org.dpbe.contract.Contract;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.CustomerRegistrationDAO;
import org.dpbe.enums.InsuranceType;
import org.dpbe.runner.ConsoleHelper;
import org.dpbe.sales.CustomerRegistration;

/**
 * UC: 고객 정보를 등록한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 판매채널은 [영업 활동] 메뉴 > [고객 등록] 항목을 클릭한다.
 *   2. 시스템은 고객 정보 등록 화면을 출력한다.
 *   3. 판매채널은 [편집] 버튼을 클릭한다.
 *   4. 시스템은 입력 폼을 편집 가능 상태로 출력한다.
 *      (이름/주민등록번호/연락처/보험종류/계약일/만료일/월보험료 필수, 주소/특약 선택)
 *   5. 판매채널은 고객 정보를 입력한다. (A1, A2)
 *   6. 판매채널은 [저장] 버튼을 클릭한다.
 *   7. 시스템은 필수 항목 누락 및 형식 오류 여부를 검증한다. (E1)
 *   8. 시스템은 중복 데이터 여부를 검증한다. (E2)
 *   9. 시스템은 고객번호와 계약번호를 자동으로 부여한다.
 *  10. 시스템은 "고객 정보가 성공적으로 등록되었습니다." 팝업 메시지를 출력한다.
 *
 * Alternative:
 *   A1) 입력 항목을 확인하고자 하는 경우
 *       → [미리보기] 버튼으로 주민등록번호 뒷자리 마스킹 토글하여 확인
 *   A2) 특약 정보를 추가 등록하고자 하는 경우
 *       → [추가] 버튼으로 특약 항목 추가
 *
 * Exception:
 *   E1) 필수 항목 누락 또는 형식 오류 발견된 경우
 *       → 오류 필드 강조 및 오류 안내 문구 출력, 나머지 값 유지 → Basic Path 7번으로 복귀
 *   E2) 중복 데이터가 감지된 경우
 *       → "이미 등록된 고객/계약번호입니다." 오류 메시지 출력 → Basic Path 7번으로 복귀
 */
public class CustomerRegistrationRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 고객 정보를 등록한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 판매채널은 [고객 등록] 항목을 클릭한다.
        ConsoleHelper.printStage("판매채널", "[영업 활동] 메뉴 > [고객 등록] 항목을 클릭합니다.");

        CustomerRegistration registration = new CustomerRegistration();

        // 2. 시스템은 고객 정보 등록 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "고객 정보 등록 화면을 출력합니다.");
        ConsoleHelper.printInfo("입력 항목: 이름(필수) / 주민등록번호(필수) / 연락처(필수) / 주소(선택)");
        ConsoleHelper.printInfo("          보험종류(필수) / 계약일(필수) / 만료일(필수) / 월보험료(필수) / 특약(선택)");

        // 3. 판매채널은 [편집] 버튼을 클릭한다.
        ConsoleHelper.printStage("판매채널", "[편집] 버튼을 클릭합니다.");

        // 4. 시스템은 입력 폼을 편집 가능 상태로 출력한다.
        registration.edit();
        ConsoleHelper.printStage("시스템", "입력 폼을 편집 가능 상태로 출력합니다.");

        // 5. 판매채널은 고객 정보를 입력한다. (A1, A2)
        ConsoleHelper.printStage("판매채널", "고객 정보를 입력합니다.");

        // 이름 (필수)
        String name = ConsoleHelper.readNonEmpty("  이름: ");
        registration.setName(name);

        // 주민등록번호 (필수) + A1) 마스킹 토글 미리보기
        String ssn = ConsoleHelper.readNonEmpty("  주민등록번호 (예: 9001011234567): ");
        registration.setSsn(ssn);
        boolean previewSsn = ConsoleHelper.readYesNo("  [A1] 주민등록번호 미리보기 (마스킹 토글)");
        if (previewSsn) {
            registration.toggleSsnMask();
            ConsoleHelper.printStage("시스템", "주민등록번호: " + registration.getMaskedSsn()
                    + " (마스킹: " + (registration.getIsSsnMasked() ? "ON" : "OFF") + ")");
            registration.toggleSsnMask(); // 확인 후 원복
        }

        // 연락처 (필수)
        String phone = ConsoleHelper.readNonEmpty("  연락처 (예: 01012345678): ");
        registration.setPhone(phone);

        // 주소 (선택)
        registration.searchAddress();
        String address = ConsoleHelper.readLine("  주소 (없으면 엔터): ");
        if (!address.isEmpty()) {
            registration.setAddress(address);
        }

        // 보험종류 (필수)
        int insuranceChoice = ConsoleHelper.readMenuChoice(
                "  보험종류를 선택하세요. (필수)",
                "생명", "건강", "자동차", "화재");
        switch (insuranceChoice) {
            case 1: registration.setInsuranceType(InsuranceType.LIFE); break;
            case 2: registration.setInsuranceType(InsuranceType.HEALTH); break;
            case 3: registration.setInsuranceType(InsuranceType.AUTO); break;
            default: registration.setInsuranceType(InsuranceType.FIRE); break;
        }

        // 계약일 / 만료일 (필수)
        registration.openCalendar();
        ConsoleHelper.printInfo("  [달력 팝업] 계약일 및 만료일을 입력합니다.");
        LocalDate contractDate = ConsoleHelper.readDate("  계약일");
        LocalDate expiryDate = ConsoleHelper.readDate("  만료일");
        registration.setContractDate(contractDate);
        registration.setExpiryDate(expiryDate);

        // 월 보험료 (필수)
        long monthlyPremium = ConsoleHelper.readLong("  월 보험료 (원): ");
        registration.setMonthlyPremium(monthlyPremium);

        // A2) 특약 정보 추가
        boolean addClause = ConsoleHelper.readYesNo("  [A2] 특약 정보를 추가하시겠습니까?");
        while (addClause) {
            String clause = ConsoleHelper.readNonEmpty("  특약 내용: ");
            registration.addSpecialClause(clause);
            addClause = ConsoleHelper.readYesNo("  특약을 추가로 등록하시겠습니까?");
        }

        // 6. 판매채널은 [저장] 버튼을 클릭한다.
        ConsoleHelper.printStage("판매채널", "[저장] 버튼을 클릭합니다.");

        // 7. 시스템은 필수 항목 누락 및 형식 오류 여부를 검증한다. (E1)
        while (true) {
            ConsoleHelper.printStage("시스템", "필수 항목 및 형식 오류 여부를 검증합니다.");
            if (!registration.validateRequired()) {
                // E1) 필수 항목 누락
                registration.highlightError();
                ConsoleHelper.printError("[E1] 필수 항목을 입력해주세요. (이름/주민등록번호/연락처/보험종류/계약일/만료일/월보험료)");
                ConsoleHelper.printInfo("입력된 나머지 항목의 값은 유지됩니다. [저장] 버튼을 다시 클릭해주세요.");
                ConsoleHelper.waitEnter();
                continue;
            }
            if (!registration.validateFormat()) {
                // E1) 형식 오류
                registration.highlightError();
                ConsoleHelper.printError("[E1] 형식 오류가 발견되었습니다. (주민등록번호: 13자리 숫자 / 연락처: 10~11자리 숫자)");
                ConsoleHelper.printInfo("입력된 나머지 항목의 값은 유지됩니다. [저장] 버튼을 다시 클릭해주세요.");
                ConsoleHelper.waitEnter();
                continue;
            }
            ConsoleHelper.printSuccess("필수 항목 및 형식 검증 완료.");

            // 8. 시스템은 중복 데이터 여부를 검증한다. (E2)
            ConsoleHelper.printStage("시스템", "중복 데이터 여부를 검증합니다.");
            boolean isDuplicate = CustomerRegistrationDAO.findAll().stream()
                    .anyMatch(r -> r.getSsn() != null && r.getSsn().equals(registration.getSsn()));
            if (isDuplicate) {
                // E2) 중복 데이터 감지 → Basic Path 7번으로 복귀
                registration.showDuplicateError();
                ConsoleHelper.printError("[E2] 이미 등록된 고객/계약번호입니다.");
                ConsoleHelper.printInfo("중복 항목을 수정한 후 [저장] 버튼을 클릭해주세요.");
                ConsoleHelper.waitEnter();
                continue;
            }
            ConsoleHelper.printSuccess("중복 검증 완료.");
            break;
        }

        // 9. 시스템은 고객번호와 계약번호를 자동으로 부여한다.
        registration.assignIds();
        registration.save();
        CustomerRegistrationDAO.save(registration);

        Customer customer = new Customer(
                registration.getName(), registration.getSsn(), registration.getPhone(), "");
        CustomerDAO.save(customer);

        Contract contract = new Contract(
                customer,
                registration.getContractDate(),
                registration.getExpiryDate(),
                registration.getMonthlyPremium());
        contract.setInsuranceType(registration.getInsuranceType().name());
        ContractDAO.save(contract);

        ConsoleHelper.printStage("시스템", "고객번호와 계약번호를 자동으로 부여합니다.");
        ConsoleHelper.printInfo("고객번호: " + customer.getCustomerNo()
                + " | 계약번호: " + contract.getContractNo());

        // 10. 시스템은 "고객 정보가 성공적으로 등록되었습니다." 팝업 메시지를 출력한다.
        registration.showSuccessPopup();
        ConsoleHelper.printStage("시스템", "고객 정보가 성공적으로 등록되었습니다.");
        String insuranceTypeStr;
        switch (registration.getInsuranceType()) {
            case LIFE: insuranceTypeStr = "생명"; break;
            case HEALTH: insuranceTypeStr = "건강"; break;
            case AUTO: insuranceTypeStr = "자동차"; break;
            default: insuranceTypeStr = "화재"; break;
        }
        ConsoleHelper.printInfo("고객번호: " + registration.getCustomerId()
                + " | 계약번호: " + registration.getContractNo());
        ConsoleHelper.printInfo("이름: " + registration.getName()
                + " | 연락처: " + registration.getPhone()
                + " | 보험종류: " + insuranceTypeStr
                + " | 월보험료: " + registration.getMonthlyPremium() + "원"
                + " | 특약: " + (registration.getSpecialClauses().isEmpty()
                ? "없음" : String.join(", ", registration.getSpecialClauses())));

        ConsoleHelper.waitEnter();
    }
}