package org.dpbe.old.runner.usecase;

import java.io.File;
import org.dpbe.domain.actor.DispatchAgent;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.domain.claim.entity.DispatchRecord;
import org.dpbe.domain.common.entity.Attachment;
import org.dpbe.old.dao.DispatchAgentDAO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.old.dao.DispatchDAO;
import org.dpbe.old.dao.DispatchRecordDAO;
import org.dpbe.domain.common.enums.DispatchStatus;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC2: 「현장 출동 정보를 기록한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 출동 직원 배정 → 도착 → 사진 업로드 → 특이사항 입력 → 전송
 * 분기:
 * - A3: 사고 위치 갱신
 * - A4: 출동 취소
 * - E1: 필수 항목(사진/특이사항) 미입력 시 전송 불가
 */
public class DispatchRecordRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC2: 현장 출동 정보를 기록한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 출동 건 선택
        Dispatch dispatch = selectDispatch();
        if (dispatch == null) return;

        // 2) 직원 배정 (이미 배정되어 있으면 건너뜀)
        if (dispatch.getAgent() == null) {
            DispatchAgent agent = selectAgent();
            if (agent == null) return;
            dispatch.assignAgent(agent);
            ConsoleHelper.printSuccess("출동 직원 배정: " + agent.getName());

            // 도착 예정 시간 입력
            LocalDateTime estimated = ConsoleHelper.readDateTime("  도착 예정 시간을 입력하세요");
            dispatch.setEstimatedArrival(estimated);
            DispatchDAO.save(dispatch);
        }

        // 3) A1/A2/A3/A4 분기 선택
        while (true) {
            int choice = ConsoleHelper.readMenuChoice(
                    "[현장출동 직원] 다음 작업을 선택하세요:",
                    "현장 출발 → 도착 → 기록 작성 (정상 흐름)",
                    "고객 위치 업데이트 확인 (A1)",
                    "현장 도착 전 고객 연락 (A2)",
                    "사고 상황 종료 → 출동 취소 (A3)",
                    "정확한 위치 정보 갱신 (A4)");
            if (choice == 1) {
                writeRecord(dispatch);
                return;
            } else if (choice == 2) {
                // A1) 고객 위치 업데이트 알림 확인
                ConsoleHelper.printInfo("[A1] 고객으로부터 위치 업데이트 알림을 수신합니다.");
                String updatedLoc = ConsoleHelper.readNonEmpty("  고객이 전달한 현재 위치: ");
                dispatch.updateLocation(updatedLoc);
                DispatchDAO.save(dispatch);
                ConsoleHelper.printSuccess("[A1] 위치 정보가 갱신되었습니다: " + updatedLoc);
            } else if (choice == 3) {
                // A2) 현장 도착 전 고객 연락
                ConsoleHelper.printInfo("[A2] 현장 도착 전 고객과 연락합니다.");
                int contactMethod = ConsoleHelper.readMenuChoice("  연락 방법:", "전화", "문자");
                String memo = ConsoleHelper.readLine("  연락 메모 (없으면 엔터): ");
                ConsoleHelper.printSuccess("[A2] 연락 완료. 방법: " + (contactMethod == 1 ? "전화" : "문자")
                        + (memo.isEmpty() ? "" : " | 메모: " + memo));
            } else if (choice == 4) {
                // A3) 사고 상황 종료로 출동 취소
                String reason = ConsoleHelper.readNonEmpty("  취소 사유: ");
                dispatch.cancel(reason);
                DispatchDAO.save(dispatch);
                ConsoleHelper.printInfo("[A3] 사고 상황 종료로 출동이 취소되었습니다. (사유: " + reason + ")");
                ConsoleHelper.waitEnter();
                return;
            } else if (choice == 5) {
                // A4) 정확한 위치 정보 갱신
                String newLoc = ConsoleHelper.readNonEmpty("  정확한 사고 위치: ");
                dispatch.updateLocation(newLoc);
                DispatchDAO.save(dispatch);
                ConsoleHelper.printSuccess("[A4] 위치 정보가 정확하게 갱신되었습니다: " + newLoc);
            }
        }
    }

    /** 정상 흐름: 출발 → 도착 → 기록 작성 → 전송 */
    private static boolean writeRecord(Dispatch dispatch) {
        ConsoleHelper.printStage("현장출동 직원", "현장으로 출발합니다.");
        dispatch.depart();

        ConsoleHelper.printStage("현장출동 직원", "현장에 도착했습니다.");
        dispatch.arrive();

        DispatchRecord record = new DispatchRecord(dispatch);

        // 사진 업로드 (시연용 - 실제로는 카메라/파일 선택)
        ConsoleHelper.printStage("현장출동 직원", "현장 사진을 업로드합니다.");
        ConsoleHelper.printInfo("(시연 목적: 더미 파일을 자동으로 업로드합니다)");

        boolean uploadAll = ConsoleHelper.readYesNo("  필수 사진(전경/파손/번호판/블랙박스)을 모두 업로드하시겠습니까?");
        if (uploadAll) {
            record.uploadPhoto("전경", new Attachment(new File("front.jpg")));
            record.uploadPhoto("파손", new Attachment(new File("damage.jpg")));
            record.uploadPhoto("번호판", new Attachment(new File("plate.jpg")));
            record.uploadPhoto("블랙박스", new Attachment(new File("blackbox.mp4")));
        } else {
            ConsoleHelper.printInfo("(일부 사진만 업로드 → E1 흐름 시뮬레이션)");
            record.uploadPhoto("전경", new Attachment(new File("front.jpg")));
        }

        // 경찰/견인
        boolean police = ConsoleHelper.readYesNo("[현장출동 직원] 경찰 출동이 필요합니까?");
        record.setPoliceRequired(police);
        boolean towing = ConsoleHelper.readYesNo("[현장출동 직원] 견인이 필요합니까?");
        record.setTowingRequired(towing);

        // 특이사항
        String notes = ConsoleHelper.readLine("[현장출동 직원] 현장 특이사항 및 소견 (Enter로 생략): ");
        if (!notes.isEmpty()) {
            record.enterNotes(notes);
        }

        // 전송 시도 (E1: 필수 항목 누락 시 실패)
        ConsoleHelper.printStage("현장출동 직원", "기록을 본사로 전송합니다.");
        if (!record.validateRequired()) {
            ConsoleHelper.printError("[E1] 필수 항목(사진/특이사항)이 누락되어 전송할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return false;
        }
        record.transmit();
        DispatchRecordDAO.save(record);
        dispatch.complete();
        DispatchDAO.save(dispatch);
        ConsoleHelper.printSuccess("기록 ID: " + record.getRecordId() + " 전송 완료");
        ConsoleHelper.waitEnter();
        return true;
    }

    private static Dispatch selectDispatch() {
        List<Dispatch> available = DispatchDAO.findAll().stream()
                .filter(d -> d.getStatus() != DispatchStatus.COMPLETED
                        && d.getStatus() != DispatchStatus.CANCELED)
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            ConsoleHelper.printError("처리할 출동 건이 없습니다. 먼저 사고를 접수해주세요.");
            ConsoleHelper.waitEnter();
            return null;
        }
        String[] options = available.stream()
                .map(d -> d.getDispatchNo()
                        + (d.getAccident() != null
                            ? " - " + (d.getAccident().getLocation() != null
                                ? d.getAccident().getLocation()
                                : d.getAccident().getReportNo())
                            : "")
                        + " [" + d.getStatus() + "]")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 처리할 출동 건을 선택하세요:", options);
        return available.get(choice - 1);
    }

    private static DispatchAgent selectAgent() {
        List<DispatchAgent> agents = DispatchAgentDAO.findAll();
        if (agents.isEmpty()) {
            ConsoleHelper.printError("배정 가능한 출동 직원이 없습니다.");
            return null;
        }
        String[] options = agents.stream()
                .map(a -> a.getName() + " (" + a.getRegion() + ", 차량 " + a.getVehicleNo() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 출동 직원을 선택하세요:", options);
        return agents.get(choice - 1);
    }
}
