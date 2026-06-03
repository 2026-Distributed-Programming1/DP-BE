package org.dpbe.domain.payment.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.actor.FinanceManager;

/**
 * 공제 수기 조정 내역 (DeductionAdjustment)
 *
 * 해약 환급금 산출 시 자동 산출된 공제 항목을 수기로 조정한 이력을 기록하는 클래스이다.
 * RefundCalculation의 집약(Aggregation) 부품으로, 한 건의 환급금 산출에 대해
 * 여러 개의 조정이 발생할 수 있다.
 */
public class DeductionAdjustment {

    private String itemName;             // 공제 항목명
    private long originalAmount;         // 원래 금액
    private long adjustedAmount;         // 조정 금액
    private LocalDateTime adjustedAt;    // 조정일시
    private FinanceManager adjustedBy;   // 조정자
    private String note;                 // 조정 메모

    /** 생성자 - adjustedAt=now() */
    public DeductionAdjustment(String item, long original, long adjusted, FinanceManager adjustedBy, String note) {
        this.itemName = item;
        this.originalAmount = original;
        this.adjustedAmount = adjusted;
        this.adjustedBy = adjustedBy;
        this.note = note;
        this.adjustedAt = LocalDateTime.now();
    }

    /** 조정 적용 */
    public void apply() {
      // 처리 필요 
    }

    // Getter
    public String getItemName() { return itemName; }
    public long getOriginalAmount() { return originalAmount; }
    public long getAdjustedAmount() { return adjustedAmount; }
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public FinanceManager getAdjustedBy() { return adjustedBy; }
    public String getNote() { return note; }
}
