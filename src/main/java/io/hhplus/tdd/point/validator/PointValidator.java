package io.hhplus.tdd.point.validator;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 포인트 관련 검증 로직 통합
 */
@Component
public class PointValidator {

    private static final long MAX_POINT = 10_000_000L;  // 최대 보유 한도: 천만원
    private static final long DAILY_CHARGE_LIMIT = 5_000_000L;  // 일일 충전 한도: 오백만원

    /**
     * 포인트 충전 전체 검증
     */
    public void validateCharge(long currentPoint, long chargeAmount, List<PointHistory> history) {
        validateAmount(chargeAmount, "충전");
        validateMaxPoint(currentPoint, chargeAmount);
        validateDailyLimit(history, chargeAmount);
    }

    /**
     * 포인트 사용 전체 검증
     */
    public void validateUse(long currentPoint, long useAmount) {
        validateAmount(useAmount, "사용");
        validateSufficientBalance(currentPoint, useAmount);
    }

    /**
     * 금액 검증
     */
    private void validateAmount(long amount, String type) {
        if (amount <= 0) {
            throw new IllegalArgumentException(type + " 금액은 0보다 커야 합니다.");
        }
    }

    /**
     * 최대 보유 한도 검증 (천만원)
     */
    private void validateMaxPoint(long currentPoint, long chargeAmount) {
        if (currentPoint + chargeAmount > MAX_POINT) {
            throw new IllegalArgumentException("최대 보유 한도(천만원)를 초과할 수 없습니다.");
        }
    }

    /**
     * 일일 충전 한도 검증 (오백만원)
     */
    private void validateDailyLimit(List<PointHistory> history, long chargeAmount) {
        long todayChargeSum = calculateTodayChargeSum(history);

        if (todayChargeSum + chargeAmount > DAILY_CHARGE_LIMIT) {
            throw new IllegalArgumentException("일일 충전 한도(오백만원)를 초과했습니다.");
        }
    }

    /**
     * 잔액 부족 검증
     */
    private void validateSufficientBalance(long currentPoint, long useAmount) {
        if (currentPoint < useAmount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
    }

    /**
     * 오늘 충전한 금액 합계 계산
     */
    private long calculateTodayChargeSum(List<PointHistory> history) {
        LocalDate today = LocalDate.now();

        return history.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .filter(h -> toLocalDate(h.updateMillis()).equals(today))
                .mapToLong(PointHistory::amount)
                .sum();
    }

    private LocalDate toLocalDate(long millis) {
        return LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(millis),
                ZoneId.systemDefault()
        );
    }
}