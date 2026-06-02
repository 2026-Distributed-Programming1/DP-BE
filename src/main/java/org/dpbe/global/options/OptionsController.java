package org.dpbe.global.options;

import java.util.List;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ClaimType;
import org.dpbe.domain.common.enums.ContractStatus;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.common.enums.InquiryType;
import org.dpbe.domain.common.enums.PaymentMethod;
import org.dpbe.domain.common.enums.PlanStatus;
import org.dpbe.global.exception.ApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/options")
public class OptionsController {

    @GetMapping("/{group}")
    public List<OptionResponse> options(@PathVariable String group) {
        return switch (group) {
            case "payment-methods" -> List.of(
                    new OptionResponse(PaymentMethod.IMMEDIATE_TRANSFER.name(), "즉시이체"),
                    new OptionResponse(PaymentMethod.VIRTUAL_ACCOUNT.name(), "가상계좌")
            );
            case "contract-statuses" -> List.of(
                    new OptionResponse(ContractStatus.NORMAL.name(), "정상"),
                    new OptionResponse(ContractStatus.EXPIRED.name(), "만기"),
                    new OptionResponse(ContractStatus.CANCELLED.name(), "해지"),
                    new OptionResponse(ContractStatus.LAPSED.name(), "실효")
            );
            case "claim-types" -> List.of(
                    new OptionResponse(ClaimType.DISEASE.name(), "질병"),
                    new OptionResponse(ClaimType.ACCIDENT.name(), "재해")
            );
            case "channel-types" -> List.of(
                    new OptionResponse(ChannelType.DESIGNER.name(), "설계사"),
                    new OptionResponse(ChannelType.AGENCY.name(), "대리점")
            );
            case "inquiry-types" -> List.of(
                    new OptionResponse(InquiryType.INSURANCE.name(), "보험료"),
                    new OptionResponse(InquiryType.CLAIM.name(), "보험금"),
                    new OptionResponse(InquiryType.CONTRACT_CHANGE.name(), "계약변경"),
                    new OptionResponse(InquiryType.CANCELLATION.name(), "해지"),
                    new OptionResponse(InquiryType.OTHER.name(), "기타")
            );
            case "inquiry-statuses" -> List.of(
                    new OptionResponse(InquiryStatus.PENDING.name(), "답변대기"),
                    new OptionResponse(InquiryStatus.ANSWERED.name(), "답변완료")
            );
            case "education-statuses" -> List.of(
                    new OptionResponse(PlanStatus.TEMP_SAVE.name(), "임시저장"),
                    new OptionResponse(PlanStatus.UNDER_REVIEW.name(), "검토중")
            );
            default -> throw ApiException.badRequest("알 수 없는 옵션 그룹입니다: " + group);
        };
    }
}
