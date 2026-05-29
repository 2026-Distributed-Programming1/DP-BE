package org.dpbe.old.runner.usecase;

import org.dpbe.domain.common.enums.ScreeningStatus;
import org.dpbe.old.dao.ChannelScreeningDAO;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.old.runner.ConsoleHelper;
import org.dpbe.domain.sales.entity.ChannelScreening;

import java.time.LocalDate;
import java.util.List;

/**
 * UC: 판매채널 채용을 심사한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 영업 관리자는 [채널 채용] 항목을 클릭한다.
 *   2. 시스템은 지원자 목록 화면을 출력한다.
 *   3. 영업 관리자는 조회 조건을 입력하고 [조회] 버튼을 클릭한다.
 *   4. 시스템은 조회 조건에 맞는 지원자 목록을 출력한다. (A3)
 *   5. 영업 관리자는 지원자 목록에서 심사할 지원자 항목을 클릭한다.
 *   6. 시스템은 지원자 상세 정보 패널을 출력한다.
 *   7. 영업 관리자는 [심사 승인] 버튼을 클릭한다. (A1, A2)
 *   8. 시스템은 "해당 지원자를 승인하시겠습니까?" 확인 팝업을 출력한다.
 *   9. 영업 관리자는 [확인] 버튼을 클릭한다.
 *  10. 시스템은 심사 승인 처리 후 인사 담당자에게 채널 등록 요청 알림
 *      (지원자명, 채널유형, 승인일시)을 자동 발송하고
 *      완료 결과(승인번호, 승인일시, 지원자명)를 출력한다. (E1, E2)
 *
 * Alternative:
 *   A1) 지원자를 거절하는 경우
 *       → 거절 사유 입력 팝업 출력 → 거절 처리 → "해당 지원자가 거절 처리되었습니다." 출력
 *   A2) [닫기] 버튼을 클릭한 경우
 *       → 상세 정보 패널을 닫고 지원자 목록 화면으로 돌아간다.
 *   A3) 조회 조건에 해당하는 지원자가 없는 경우
 *       → "조회 가능한 지원자가 없습니다." 메시지 출력
 *
 * Exception:
 *   E1) 채널 등록 요청 알림 발송 실패 시
 *       → "알림 발송에 실패했습니다. 다시 시도해 주세요." 팝업 출력
 *   E2) 채널 등록 처리 실패 시
 *       → "채널 등록 처리에 실패했습니다. 다시 시도해 주세요." 팝업 출력
 */
public class ChannelScreeningRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 판매채널 채용을 심사한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 영업 관리자는 [채널 채용] 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "[영업] 메뉴 > [채널 채용] 항목을 클릭합니다.");

        ChannelScreening screening = new ChannelScreening();

        // 2. 시스템은 지원자 목록 화면을 출력한다.
        screening.loadApplicantList();
        ConsoleHelper.printStage("시스템", "지원자 목록 화면을 출력합니다.");
        ConsoleHelper.printInfo("테이블 컬럼: 지원자명 / 채널유형 / 지원일 / 경력 / 자격증 / 심사상태");
        List<ChannelScreening> screeningList = ChannelScreeningDAO.findAll();
        if (screeningList.isEmpty()) {
            ConsoleHelper.printInfo("  (저장된 지원자 데이터가 없습니다.)");
        } else {
            ConsoleHelper.printInfo("  번호 | 지원자명 | 채널유형 | 지원일 | 경력 | 자격증 | 심사상태");
            for (int i = 0; i < screeningList.size(); i++) {
                ChannelScreening s = screeningList.get(i);
                String ct = s.getChannelType() != null ? (s.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점") : "-";
                ConsoleHelper.printInfo("  " + (i + 1) + " | " + s.getApplicantName()
                        + " | " + ct
                        + " | " + (s.getApplicationDate() != null ? s.getApplicationDate() : "-")
                        + " | " + (s.getCareer() != null ? s.getCareer() : "없음")
                        + " | " + (s.getCertifications().isEmpty() ? "없음" : String.join(", ", s.getCertifications()))
                        + " | " + s.getScreeningStatus());
            }
        }

        // 3. 영업 관리자는 조회 조건을 입력하고 [조회] 버튼을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "조회 조건을 입력합니다.");
        LocalDate filterStart = ConsoleHelper.readDate("  조회 시작일");
        LocalDate filterEnd = ConsoleHelper.readDate("  조회 종료일");
        screening.setFilterStartDate(filterStart);
        screening.setFilterEndDate(filterEnd);

        int typeFilter = ConsoleHelper.readMenuChoice("  채널 유형 필터:", "전체", "설계사", "대리점");
        int statusFilter = ConsoleHelper.readMenuChoice("  심사 상태 필터:", "전체", "대기", "승인", "거절");
        screening.search();

        // 조회 조건으로 목록 필터링
        List<ChannelScreening> filtered = screeningList.stream()
                .filter(s -> s.getApplicationDate() == null
                        || (!s.getApplicationDate().isBefore(filterStart) && !s.getApplicationDate().isAfter(filterEnd)))
                .filter(s -> typeFilter == 1 || s.getChannelType() != null
                        && ((typeFilter == 2 && s.getChannelType() == ChannelType.DESIGNER)
                            || (typeFilter == 3 && s.getChannelType() != ChannelType.DESIGNER)))
                .filter(s -> statusFilter == 1 || s.getScreeningStatus() != null
                        && ((statusFilter == 2 && s.getScreeningStatus() == ScreeningStatus.PENDING)
                            || (statusFilter == 3 && s.getScreeningStatus() == ScreeningStatus.APPROVED)
                            || (statusFilter == 4 && s.getScreeningStatus() == ScreeningStatus.REJECTED)))
                .collect(java.util.stream.Collectors.toList());

        // 4. 시스템은 조회 조건에 맞는 지원자 목록을 출력한다. (A3)
        ConsoleHelper.printStage("시스템", "조회 조건에 맞는 지원자 목록을 출력합니다.");
        if (filtered.isEmpty()) {
            // A3) 조회 조건에 해당하는 지원자가 없는 경우
            ConsoleHelper.printStage("시스템", "조회 가능한 지원자가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 지원자 정보 입력 (조회 결과 시뮬레이션)
        ConsoleHelper.printStage("영업관리자", "지원자 정보를 입력합니다.");
        String applicantName = ConsoleHelper.readNonEmpty("  지원자명: ");
        screening.setApplicantName(applicantName);

        int typeChoice = ConsoleHelper.readMenuChoice(
                "  채널 유형을 선택하세요.",
                "설계사", "대리점");
        screening.setChannelType(typeChoice == 1 ? ChannelType.DESIGNER : ChannelType.AGENCY);

        String career = ConsoleHelper.readLine("  경력 사항 (없으면 엔터): ");
        if (!career.isEmpty()) {
            screening.setCareer(career);
        }

        String cert = ConsoleHelper.readLine("  자격증 (없으면 엔터): ");
        if (!cert.isEmpty()) {
            screening.addCertification(cert);
        }

        // 5. 영업 관리자는 지원자 목록에서 심사할 지원자 항목을 클릭한다.
        ConsoleHelper.printStage("영업관리자", "심사할 지원자 항목을 클릭합니다.");

        // 6. 시스템은 지원자 상세 정보 패널을 출력한다.
        screening.openDetailPanel();
        ConsoleHelper.printStage("시스템", "지원자 상세 정보 패널을 출력합니다.");
        String channelTypeStr = screening.getChannelType() == ChannelType.DESIGNER ? "설계사" : "대리점";
        ConsoleHelper.printInfo("지원자명: " + screening.getApplicantName()
                + " | 채널유형: " + channelTypeStr
                + " | 지원일: " + screening.getApplicationDate()
                + " | 경력: " + (screening.getCareer() != null ? screening.getCareer() : "없음")
                + " | 자격증: " + (screening.getCertifications().isEmpty() ? "없음" : String.join(", ", screening.getCertifications())));

        // 7. 영업 관리자는 [심사 승인], [심사 거절], [닫기] 중 선택한다. (A1, A2)
        int action = ConsoleHelper.readMenuChoice(
                "[영업관리자] 처리를 선택하세요.",
                "심사 승인", "심사 거절", "닫기");

        if (action == 3) {
            // A2) [닫기] 버튼을 클릭한 경우
            screening.closeDetailPanel();
            ConsoleHelper.printStage("시스템", "상세 정보 패널을 닫고 지원자 목록 화면으로 돌아갑니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        if (action == 2) {
            // A1) 지원자를 거절하는 경우
            // 시스템은 거절 사유 입력 팝업을 출력한다.
            screening.openRejectionPopup();
            ConsoleHelper.printStage("시스템", "거절 사유 입력 팝업을 출력합니다.");
            String rejectionReason = ConsoleHelper.readNonEmpty("  [A1] 거절 사유: ");
            screening.setRejectionReason(rejectionReason);
            // 영업 관리자는 거절 사유를 입력하고 [확인] 버튼을 클릭한다.
            screening.reject();
            ChannelScreeningDAO.save(screening);
            // 시스템은 "해당 지원자가 거절 처리되었습니다." 메시지를 출력한다.
            ConsoleHelper.printStage("시스템", "해당 지원자가 거절 처리되었습니다.");
            ConsoleHelper.printInfo("지원자명: " + screening.getApplicantName()
                    + " | 거절 사유: " + screening.getRejectionReason());
            ConsoleHelper.waitEnter();
            return;
        }

        // 8. 시스템은 "해당 지원자를 승인하시겠습니까?" 확인 팝업을 출력한다.
        screening.showApprovalConfirm();
        ConsoleHelper.printStage("시스템", "해당 지원자를 승인하시겠습니까?");

        // 9. 영업 관리자는 [확인] 버튼을 클릭한다.
        boolean confirm = ConsoleHelper.readYesNo("  확인");
        if (!confirm) {
            ConsoleHelper.waitEnter();
            return;
        }

        // 10. 시스템은 심사 승인 처리 후 인사 담당자에게 채널 등록 요청 알림을 자동 발송하고
        //     완료 결과(승인번호, 승인일시, 지원자명)를 출력한다. (E1, E2)
        screening.approve();

        // E2) 채널 등록 처리 실패 시 (시뮬레이션: 항상 성공으로 처리)
        boolean registrationSuccess = true;
        if (!registrationSuccess) {
            screening.showRegistrationError();
            ConsoleHelper.printStage("시스템", "채널 등록 처리에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 인사 담당자에게 채널 등록 요청 알림 자동 발송
        screening.notifyHr();

        // E1) 채널 등록 요청 알림 발송 실패 시 (시뮬레이션: 항상 성공으로 처리)
        boolean notifySuccess = true;
        if (!notifySuccess) {
            screening.showNotifyError();
            ConsoleHelper.printStage("시스템", "알림 발송에 실패했습니다. 다시 시도해 주세요.");
            ConsoleHelper.waitEnter();
            return;
        }

        ConsoleHelper.printStage("시스템", "인사 담당자에게 채널 등록 요청 알림을 자동 발송합니다.");
        ConsoleHelper.printInfo("지원자명: " + screening.getApplicantName()
                + " | 채널유형: " + channelTypeStr
                + " | 승인일시: " + screening.getApprovedAt());

        // 완료 결과 출력
        ChannelScreeningDAO.save(screening);
        screening.showApprovalResult();
        ConsoleHelper.printStage("시스템", "완료 결과를 출력합니다.");
        ConsoleHelper.printInfo("승인번호: " + screening.getApprovalNo()
                + " | 승인일시: " + screening.getApprovedAt()
                + " | 지원자명: " + screening.getApplicantName());

        ConsoleHelper.waitEnter();
    }
}