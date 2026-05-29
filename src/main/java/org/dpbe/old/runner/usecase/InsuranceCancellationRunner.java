package org.dpbe.old.runner.usecase;

import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.old.dao.CancellationDAO;
import org.dpbe.old.dao.ContractDAO;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.ContractStatus;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * 「보험을 해지한다」 시나리오 진행자
 *
 * 가입 보험을 조회한다 유스케이스의 A3에서 호출된다.
 *
 * 정상 흐름:
 * 1. (가입보험 조회 A3 → 여기로 이관)
 * 2. 해지 사유 입력 폼
 * 3. 해지 유의사항 확인
 * 4. 본인 인증 (E1)
 * 5. 확인 페이지 (예상 환급금)
 * 6. 해약하기 버튼 (E2)
 * 7. 완료 → 가입보험 조회 Basic Path 2번으로 이동
 *
 * 분기:
 * - A1: 해지 사유 '기타' 선택 → 상세 사유 필수 입력
 * - A2: 취소 버튼 클릭 → 가입보험 상세 화면으로 돌아가기
 *
 * @return true=해약 완료(목록으로 이동), false=취소/중단(상세 화면 유지)
 */
public class InsuranceCancellationRunner {

    static boolean run(Contract contract) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("보험을 해지한다");
        ConsoleHelper.printDoubleDivider();

        Cancellation cancellation = new Cancellation(contract);

        // Step 2: 해지 사유 선택
        String[] reasons = {"경제적 사정", "타사 가입", "서비스 불만", "보장 내용 불필요", "기타"};
        int reasonChoice = ConsoleHelper.readMenuChoice("[고객] 해지 사유를 선택하세요:", reasons);
        String reason = reasons[reasonChoice - 1];
        cancellation.selectReason(reason);

        // A1: 기타 선택 시 상세 사유 필수 입력
        if (reasonChoice == 5) {
            String detail = ConsoleHelper.readNonEmpty("[고객] 상세 사유를 입력하세요: ");
            cancellation.enterDetailReason(detail);
            if (!cancellation.validateReasonInput()) {
                ConsoleHelper.printError("상세 사유를 입력해야 합니다.");
                ConsoleHelper.waitEnter();
                return false;
            }
        }

        // A2: 취소 버튼 (사유 선택 후)
        if (ConsoleHelper.readYesNo("[고객] '취소' 버튼을 누르시겠습니까? (A2 - 해약 중단)")) {
            cancellation.cancel();
            ConsoleHelper.printInfo("[A2] 해약을 취소하고 보험 상세 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return false;
        }

        // Step 4: 해지 유의사항 확인
        ConsoleHelper.printDivider();
        System.out.println("  [유의사항]");
        System.out.println("  해약 시점부터 사고 보장이 중단되며, 납입한 보험료보다 해약 환급금이 적거나 없을 수 있습니다.");
        ConsoleHelper.printDivider();

        if (!ConsoleHelper.readYesNo("[고객] '위 유의사항을 모두 읽고 이해했습니다.' 에 동의하시겠습니까?")) {
            ConsoleHelper.printInfo("유의사항에 동의하지 않아 해약이 취소됩니다.");
            ConsoleHelper.waitEnter();
            return false;
        }
        cancellation.agreeToNotice();

        // A2: 본인 인증 단계에서 취소
        if (ConsoleHelper.readYesNo("[고객] '취소' 버튼을 누르시겠습니까? (A2 - 해약 중단)")) {
            cancellation.cancel();
            ConsoleHelper.printInfo("[A2] 해약을 취소하고 보험 상세 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return false;
        }

        // Step 5: 본인 인증 (E1)
        while (true) {
            ConsoleHelper.printInfo("[시스템] 본인 인증을 진행합니다.");
            boolean simulateFail = ConsoleHelper.readYesNo("  [E1 시뮬레이션] 본인 인증 실패를 시뮬레이션하시겠습니까?");
            if (simulateFail) {
                ConsoleHelper.printError("[E1] 본인 인증에 실패하였습니다. 다시 시도해 주세요.");
                if (!ConsoleHelper.readYesNo("[고객] 다시 시도하시겠습니까?")) {
                    return false;
                }
            } else {
                cancellation.authenticate();
                ConsoleHelper.printSuccess("본인 인증 성공");
                break;
            }
        }

        // Step 6: 확인 페이지
        cancellation.calculateExpectedRefund();
        ConsoleHelper.printDivider();
        System.out.println("  [신청 내역]");
        System.out.println("  보험명     : " + (contract.getInsuranceType() != null ? contract.getInsuranceType() : "(미설정)"));
        System.out.println("  증권 번호  : " + contract.getPolicyNo());
        System.out.println("  해약 사유  : " + reason);
        System.out.println("  예상 환급금: " + String.format("%,d", cancellation.getExpectedRefund()) + "원");
        System.out.println("  * 해약이 완료되면 담당자가 내용을 확인한 후, 2~3일 내에 최종 처리 및 환급 보험료 지급을 진행합니다.");
        ConsoleHelper.printDivider();

        // A2: 확인 페이지에서 취소
        if (!ConsoleHelper.readYesNo("[고객] '해약하기' 버튼을 클릭하시겠습니까?")) {
            cancellation.cancel();
            ConsoleHelper.printInfo("[A2] 해약을 취소하고 보험 상세 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return false;
        }

        // Step 7: 해약 신청 (E2 시뮬레이션)
        boolean simulateError = ConsoleHelper.readYesNo("  [E2 시뮬레이션] 해약 신청 처리 오류를 시뮬레이션하시겠습니까?");
        if (simulateError) {
            cancellation.handleSubmitError();
            ConsoleHelper.printError("[E2] 해약 신청 처리 중 오류가 발생하였습니다. 잠시 후 다시 시도하거나 고객센터(0000-0000)로 문의해 주세요.");
            int retryChoice = ConsoleHelper.readMenuChoice("[고객] 처리를 선택하세요:", "다시 시도", "메인으로 가기");
            if (retryChoice == 2) {
                ConsoleHelper.waitEnter();
                return false;
            }
            // 다시 시도: E2 미시뮬레이션으로 바로 진행
        }

        // Step 8: 해약 완료
        cancellation.submit();
        contract.setStatus(ContractStatus.CANCELLED);
        DBA.beginTransaction();
        try {
            CancellationDAO.save(cancellation);
            ContractDAO.save(contract);
            DBA.commit();
        } catch (Exception e) {
            DBA.rollback();
            ConsoleHelper.printError("해지 처리 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
            ConsoleHelper.waitEnter();
            return false;
        }
        ConsoleHelper.printSuccess("보험 해약이 완료되었습니다.");
        ConsoleHelper.printInfo("해지번호: " + cancellation.getCancellationNo()
                + " | 해지일시: " + cancellation.getCanceledAt());
        ConsoleHelper.printInfo("환급금은 추후 별도 안내 드리겠습니다.");
        ConsoleHelper.waitEnter();

        return true;
    }
}