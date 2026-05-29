package org.dpbe.runner.usecase;

import org.dpbe.actor.Customer;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.InquiryDAO;
import org.dpbe.enums.FaqCategory;
import org.dpbe.enums.InquiryStatus;
import org.dpbe.enums.InquiryType;
import org.dpbe.inquiry.CustomerCenterPage;
import java.util.List;
import org.dpbe.inquiry.Inquiry;
import org.dpbe.runner.ConsoleHelper;

/**
 * UC: 문의한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 고객은 [고객센터] 버튼을 클릭한다.
 *   2. 고객은 [1:1 문의] 탭을 클릭한다. (A1, A2)
 *   3. 고객은 문의 유형 드롭다운에서 하나를 선택한다.
 *   4. 고객은 제목 입력란에 문의 제목을 입력한다.
 *   5. 고객은 내용 입력란에 문의 내용을 입력한다. (A3)
 *   6. 고객은 [제출] 버튼을 클릭한다. (E1)
 *   7. 시스템은 문의 유형, 제목, 내용, 첨부 파일을 저장하고 고유한 문의 번호를 부여한다.
 *   8. 시스템은 "문의가 정상적으로 접수되었습니다. 문의 번호: [문의 번호]" 팝업을 출력한다.
 *   9. 시스템은 고객 연락처로 문의 접수 확인 문자를 발송한다.
 *  10. 방금 접수된 문의가 문의 내역 목록 최상단에 '답변 대기' 상태로 표시된다.
 *  11. 고객은 해당 항목을 클릭한다. (A4)
 *  12. 시스템은 문의 상세 페이지를 표시한다.
 *
 * Alternative:
 *   A1) [자주 묻는 질문] 탭을 클릭하는 경우
 *       → 카테고리 선택 → FAQ 필터링 → 항목 클릭 → 답변 펼치기
 *   A2) [문의 내역 조회] 탭을 클릭하는 경우
 *       → 문의 항목 클릭하여 상세 조회
 *   A3) 파일을 첨부하는 경우
 *       → 파일 탐색기 열기 → 파일 선택 (E2) → 파일명/크기 표시
 *   A4) 답변 완료된 문의를 확인하는 경우
 *       → 처리 상태가 '답변 완료'인 문의 항목 클릭
 *
 * Exception:
 *   E1) 필수 항목을 입력하지 않은 경우
 *       → 입력란 빨간색 강조 및 "필수 입력 항목입니다." 문구 출력
 *   E2) 첨부 파일이 허용 용량(10MB)을 초과한 경우
 *       → "첨부 파일은 10MB 이하만 업로드 가능합니다." 문구 출력
 */
public class InquiryRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 문의한다");
        ConsoleHelper.printDoubleDivider();

        // 고객 선택
        List<Customer> customers = CustomerDAO.findAll();
        if (customers.isEmpty()) {
            ConsoleHelper.printError("등록된 고객이 없습니다.");
            return;
        }
        String[] customerOptions = customers.stream()
                .map(c -> c.getName() + " (" + c.getCustomerNo() + ")")
                .toArray(String[]::new);
        int customerChoice = ConsoleHelper.readMenuChoice("[시스템] 고객을 선택하세요:", customerOptions);
        Customer customer = customers.get(customerChoice - 1);

        // 1. 고객은 [고객센터] 버튼을 클릭한다.
        ConsoleHelper.printStage("고객", "[고객센터] 버튼을 클릭합니다.");

        CustomerCenterPage page = new CustomerCenterPage();

        // 2. 고객은 탭을 선택한다. (A1, A2)
        page.switchTab();
        ConsoleHelper.printStage("시스템", "고객센터 페이지를 출력합니다.");

        int tabChoice = ConsoleHelper.readMenuChoice(
                "[고객] 탭을 선택하세요.",
                "1:1 문의", "자주 묻는 질문 (A1)", "문의 내역 조회 (A2)");

        if (tabChoice == 2) {
            // A1) [자주 묻는 질문] 탭을 클릭하는 경우
            Inquiry faqInquiry = new Inquiry();
            page.switchTab();
            ConsoleHelper.printStage("고객", "[A1] [자주 묻는 질문] 탭을 클릭합니다.");

            // 카테고리 선택
            int categoryChoice = ConsoleHelper.readMenuChoice(
                    "  카테고리를 선택하세요.",
                    "전체", "보험료", "보험금", "계약변경", "해지", "기타");
            FaqCategory[] categories = {
                    FaqCategory.ALL, FaqCategory.INSURANCE, FaqCategory.CLAIM,
                    FaqCategory.CONTRACT_CHANGE, FaqCategory.CANCELLATION, FaqCategory.OTHER
            };
            faqInquiry.setFaqCategory(categories[categoryChoice - 1]);

            // 시스템은 선택한 카테고리에 해당하는 FAQ 항목만 필터링해 표시
            faqInquiry.filterFaqByCategory();
            ConsoleHelper.printStage("시스템", "선택한 카테고리의 FAQ 항목을 필터링하여 표시합니다.");

            // 고객은 FAQ 항목 중 하나를 클릭 → 답변 펼치기
            ConsoleHelper.printStage("고객", "FAQ 항목을 클릭합니다.");
            faqInquiry.toggleFaqItem();
            ConsoleHelper.printStage("시스템", "해당 항목의 답변 내용을 펼쳐서 표시합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        if (tabChoice == 3) {
            // A2) [문의 내역 조회] 탭을 클릭하는 경우 (BUG-INQ-02)
            page.switchTab();
            ConsoleHelper.printStage("고객", "[A2] [문의 내역 조회] 탭을 클릭합니다.");
            List<Inquiry> histories = InquiryDAO.findByCustomerName(customer.getName());
            ConsoleHelper.printStage("시스템", "문의 내역 목록을 출력합니다.");
            if (histories.isEmpty()) {
                ConsoleHelper.printInfo("문의 내역이 없습니다.");
            } else {
                for (int i = 0; i < histories.size(); i++) {
                    Inquiry h = histories.get(i);
                    String hType = h.getInquiryType() != null ? h.getInquiryType().name() : "-";
                    System.out.println("  [" + (i + 1) + "] " + h.getTitle()
                            + " | 유형: " + hType
                            + " | 접수일시: " + h.getReceivedAt()
                            + " | 상태: " + (h.getStatus() == InquiryStatus.PENDING ? "답변 대기" : "답변 완료"));
                }
                String[] histTitles = histories.stream()
                        .map(h -> h.getTitle() != null ? h.getTitle() : "(제목 없음)")
                        .toArray(String[]::new);
                int histChoice = ConsoleHelper.readMenuChoice("[고객] 확인할 문의를 선택하세요.", histTitles);
                Inquiry selected = histories.get(histChoice - 1);
                ConsoleHelper.printStage("시스템", "문의 상세 페이지를 표시합니다.");
                ConsoleHelper.printInfo("문의 번호: " + selected.getInquiryNo()
                        + " | 제목: " + selected.getTitle()
                        + " | 내용: " + selected.getContent()
                        + " | 첨부: " + (selected.getAttachmentFileName() != null ? selected.getAttachmentFileName() : "없음")
                        + " | 답변: " + (selected.getAnswerContent() != null ? selected.getAnswerContent() : "미답변")
                        + " | 답변일시: " + (selected.getAnsweredAt() != null ? selected.getAnsweredAt() : "-"));
            }
            ConsoleHelper.waitEnter();
            return;
        }

        // Basic Path - [1:1 문의] 탭
        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerName(customer.getName());
        ConsoleHelper.printStage("고객", "[1:1 문의] 탭을 클릭합니다.");

        // BUG-INQ-04 E1: 필수 항목 검증 실패 시 재입력 루프
        boolean valid = false;
        while (!valid) {
            // 3. 고객은 문의 유형 드롭다운에서 하나를 선택한다.
            ConsoleHelper.printStage("고객", "문의 유형을 선택합니다.");
            int typeChoice = ConsoleHelper.readMenuChoice(
                    "  문의 유형을 선택하세요. (필수)",
                    "보험료", "보험금", "계약변경", "해지", "기타");
            switch (typeChoice) {
                case 1: inquiry.setInquiryType(InquiryType.INSURANCE); break;
                case 2: inquiry.setInquiryType(InquiryType.CLAIM); break;
                case 3: inquiry.setInquiryType(InquiryType.CONTRACT_CHANGE); break;
                case 4: inquiry.setInquiryType(InquiryType.CANCELLATION); break;
                default: inquiry.setInquiryType(InquiryType.OTHER); break;
            }

            // 4. 고객은 제목 입력란에 문의 제목을 입력한다.
            String title = ConsoleHelper.readNonEmpty("  문의 제목 (최대 50자): ");
            inquiry.setTitle(title);
            ConsoleHelper.printInfo("  현재 입력 글자 수: " + inquiry.getCurrentLength() + "/50");

            // 5. 고객은 내용 입력란에 문의 내용을 입력한다. (A3)
            String content = ConsoleHelper.readNonEmpty("  문의 내용 (최대 1000자): ");
            inquiry.setContent(content);

            // A3) 파일을 첨부하는 경우 (BUG-INQ-04 E2: 재첨부 루프)
            while (ConsoleHelper.readYesNo("  [A3] 파일을 첨부하시겠습니까?")) {
                ConsoleHelper.printStage("시스템", "파일 탐색기를 엽니다.");
                String fileName = ConsoleHelper.readNonEmpty("  첨부 파일명 (예: document.pdf): ");
                long fileSize = ConsoleHelper.readLong("  첨부 파일 크기 (bytes): ");
                inquiry.setAttachmentFileName(fileName);
                inquiry.setAttachmentFileSize(fileSize);
                if (!inquiry.validateFileSize()) {
                    inquiry.showFileSizeError();
                    ConsoleHelper.printError("[E2] 첨부 파일은 10MB 이하만 업로드 가능합니다.");
                    inquiry.removeFile();
                    inquiry.setAttachmentFileName(null);
                    inquiry.setAttachmentFileSize(null);
                } else {
                    inquiry.attachFile();
                    ConsoleHelper.printStage("시스템", "파일명: " + fileName
                            + " | 크기: " + fileSize + " bytes");
                    break;
                }
            }

            // 6. 고객은 [제출] 버튼을 클릭한다. (E1)
            ConsoleHelper.printStage("고객", "[제출] 버튼을 클릭합니다.");
            ConsoleHelper.printStage("시스템", "필수 항목 누락 여부를 검증합니다.");
            if (!inquiry.validateRequired()) {
                inquiry.highlightError();
                ConsoleHelper.printError("[E1] 필수 입력 항목입니다. (문의 유형 / 제목 / 내용)");
                ConsoleHelper.printInfo("누락된 항목을 입력한 후 [제출] 버튼을 다시 클릭해주세요.");
                continue;
            }
            valid = true;
        }

        // 7. 시스템은 문의 정보를 저장하고 고유한 문의 번호를 부여한다.
        inquiry.submit();
        InquiryDAO.save(inquiry);
        ConsoleHelper.printStage("시스템", "문의 정보를 저장하고 문의 번호를 부여합니다.");

        // 8. 시스템은 접수 완료 팝업을 출력한다.
        inquiry.showSuccessPopup();
        ConsoleHelper.printStage("시스템", "문의가 정상적으로 접수되었습니다. 문의 번호: " + inquiry.getInquiryNo());

        // 9. 시스템은 고객 연락처로 접수 확인 문자를 발송한다.
        inquiry.sendConfirmSms();
        ConsoleHelper.printStage("시스템", "고객 연락처로 문의 접수 확인 문자를 발송합니다.");
        ConsoleHelper.printInfo("문의 번호: " + inquiry.getInquiryNo()
                + " | 접수 일시: " + inquiry.getReceivedAt());

        // 10. 방금 접수된 문의가 문의 내역 목록 최상단에 '답변 대기' 상태로 표시된다.
        ConsoleHelper.printStage("시스템", "문의 내역 목록 최상단에 '답변 대기' 상태로 표시됩니다.");

        // 11. 고객은 해당 항목을 클릭한다. (A4)
        int detailChoice = ConsoleHelper.readMenuChoice(
                "[고객] 처리를 선택하세요.",
                "문의 상세 조회", "답변 완료 문의 확인 (A4)");

        if (detailChoice == 2) {
            // A4) 답변 완료된 문의를 확인하는 경우 (BUG-INQ-03)
            ConsoleHelper.printStage("고객", "[A4] 처리 상태가 '답변 완료'인 문의 항목을 클릭합니다.");
            List<Inquiry> answered = InquiryDAO.findByCustomerNameAndStatus(
                    customer.getName(), InquiryStatus.ANSWERED);
            if (answered.isEmpty()) {
                ConsoleHelper.printInfo("답변 완료된 문의가 없습니다.");
            } else {
                for (int i = 0; i < answered.size(); i++) {
                    Inquiry a = answered.get(i);
                    System.out.println("  [" + (i + 1) + "] " + a.getTitle()
                            + " | 답변일시: " + a.getAnsweredAt());
                }
                String[] answeredTitles = answered.stream()
                        .map(a -> a.getTitle() != null ? a.getTitle() : "(제목 없음)")
                        .toArray(String[]::new);
                int answeredChoice = ConsoleHelper.readMenuChoice("[고객] 확인할 문의를 선택하세요.", answeredTitles);
                Inquiry selectedAnswered = answered.get(answeredChoice - 1);
                ConsoleHelper.printStage("시스템", "문의 상세 페이지를 표시합니다.");
                ConsoleHelper.printInfo("제목: " + selectedAnswered.getTitle()
                        + " | 답변: " + (selectedAnswered.getAnswerContent() != null ? selectedAnswered.getAnswerContent() : "-")
                        + " | 답변일시: " + (selectedAnswered.getAnsweredAt() != null ? selectedAnswered.getAnsweredAt() : "-"));
            }
        } else {
            // 12. 시스템은 문의 상세 페이지를 표시한다.
            inquiry.getDetail();
            ConsoleHelper.printStage("시스템", "문의 상세 페이지를 표시합니다.");
            String inquiryTypeStr;
            switch (inquiry.getInquiryType()) {
                case INSURANCE: inquiryTypeStr = "보험료"; break;
                case CLAIM: inquiryTypeStr = "보험금"; break;
                case CONTRACT_CHANGE: inquiryTypeStr = "계약변경"; break;
                case CANCELLATION: inquiryTypeStr = "해지"; break;
                default: inquiryTypeStr = "기타"; break;
            }
            ConsoleHelper.printInfo("문의 번호: " + inquiry.getInquiryNo()
                    + " | 문의 유형: " + inquiryTypeStr
                    + " | 제목: " + inquiry.getTitle()
                    + " | 접수 일시: " + inquiry.getReceivedAt()
                    + " | 처리 상태: " + (inquiry.getStatus() == InquiryStatus.PENDING ? "답변 대기" : "답변 완료"));
        }

        ConsoleHelper.waitEnter();
    }
}
