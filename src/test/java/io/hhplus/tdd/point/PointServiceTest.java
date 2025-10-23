package io.hhplus.tdd.point;

import io.hhplus.tdd.repository.PointRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

/**
 * PointService 단위 테스트
 * Given-When-Then을 명확하게 구분하여 작성 시도
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 단위 테스트")
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("특정 유저의 포인트를 조회할 수 있다")
    void getPoint_Success() {
        // given
        long userId = 1L;
        long existingPoint = 1000L;
        UserPoint expectedUserPoint = new UserPoint(userId, existingPoint, System.currentTimeMillis());

        when(pointRepository.findById(userId)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(existingPoint);
        verify(pointRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 유저의 포인트 조회 시 빈 UserPoint를 반환한다")
    void getPoint_NonExistentUser() {
        // given
        long nonExistUserId = 999L;

        // when
        when(pointRepository.findById(nonExistUserId)).thenReturn(UserPoint.empty(nonExistUserId));
        UserPoint result = pointService.getPoint(nonExistUserId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(nonExistUserId);
        assertThat(result.point()).isEqualTo(0L);
        verify(pointRepository, times(1)).findById(nonExistUserId);
    }

    @Test
    @DisplayName("특정 유저의 포인트 히스토리를 조회할 수 있다")
    void getHistory_Success() {
        // given
        long userId = 1L;
        List<PointHistory> expectedHistory = List.of(
            new PointHistory(1L, userId, 500L, TransactionType.CHARGE, System.currentTimeMillis()),
            new PointHistory(2L, userId, 200L, TransactionType.USE, System.currentTimeMillis())
        );

        // when
        when(pointRepository.findHistoryByUserId(userId)).thenReturn(expectedHistory);
        List<PointHistory> result = pointService.getHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).amount()).isEqualTo(200L);
        verify(pointRepository, times(1)).findHistoryByUserId(userId);
    }

    @Test
    @DisplayName("히스토리가 없는 유저는 빈 리스트를 반환한다")
    void getHistory_EmptyList() {
        // given
        long userId = 999L;
        when(pointRepository.findHistoryByUserId(userId)).thenReturn(List.of());

        // when
        List<PointHistory> result = pointService.getHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(pointRepository, times(1)).findHistoryByUserId(userId);
    }

    @Test
    @DisplayName("포인트를 정상적으로 충전할 수 있다")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long currentAmount = 1000L;
        long chargeAmount = 500L;
        long expectedAmount = 1500L;

        UserPoint currentPoint = new UserPoint(userId, currentAmount, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, expectedAmount, System.currentTimeMillis());

        when(pointRepository.findById(userId)).thenReturn(currentPoint);
        when(pointRepository.save(userId, expectedAmount)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(1500L);
        verify(pointRepository, times(1)).findById(userId);
        verify(pointRepository, times(1)).save(userId, expectedAmount);
        verify(pointRepository, times(1)).saveHistory(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE));
    }

    @Test
    @DisplayName("충전 금액이 0원일 경우 예외가 발생한다")
    void chargePoint_InvalidAmount_Zero() {
        // given
        long userId = 1L;
        long invalidAmount = 0L;

        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(pointRepository.findById(userId)).thenReturn(currentPoint);

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        // then: 검증 실패로 업데이트 안됨
        verify(pointRepository, times(1)).findById(userId);

        verify(pointRepository, never()).save(userId, invalidAmount);
        verify(pointRepository, never()).saveHistory(eq(userId), eq(invalidAmount)
                , eq(TransactionType.CHARGE));
    }

    @Test
    @DisplayName("충전 금액이 음수일 경우 예외가 발생한다")
    void chargePoint_InvalidAmount_Negative() {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

        when(pointRepository.findById(userId)).thenReturn(currentPoint);

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");

        // then: 검증 실패로 업데이트 안됨
        verify(pointRepository, times(1)).findById(userId);

        verify(pointRepository, never()).save(userId, invalidAmount);
        verify(pointRepository, never()).saveHistory(eq(userId), eq(invalidAmount)
                , eq(TransactionType.CHARGE));
    }

    @Test
    @DisplayName("포인트를 정상적으로 사용할 수 있다")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long currentAmount = 1000L;
        long useAmount = 300L;
        long expectedAmount = 700L;

        UserPoint currentPoint = new UserPoint(userId, currentAmount, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, expectedAmount, System.currentTimeMillis());

        when(pointRepository.findById(userId)).thenReturn(currentPoint);
        when(pointRepository.save(userId,expectedAmount)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.usePoint(userId, useAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(expectedAmount);
        verify(pointRepository, times(1)).findById(userId);
        verify(pointRepository, times(1)).save(userId, expectedAmount);
        verify(pointRepository, times(1)).saveHistory(eq(userId), eq(useAmount), eq(TransactionType.USE));
    }

    @Test
    @DisplayName("사용 금액이 0일 경우 예외가 발생한다")
    void usePoint_InvalidAmount_Zero() {
        // given
        long userId = 1L;
        long invalidAmount = 0L;

        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

        when(pointRepository.findById(userId)).thenReturn(currentPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다.");

        // then: 검증 실패로 업데이트 안됨
        verify(pointRepository, times(1)).findById(userId);
        verify(pointRepository, never()).save(userId, invalidAmount);
        verify(pointRepository, never()).saveHistory(eq(userId), eq(invalidAmount), eq(TransactionType.USE));
    }

    @Test
    @DisplayName("사용 금액이 음수일 경우 예외가 발생한다")
    void usePoint_InvalidAmount_Negative() {
        // given
        long userId = 1L;
        long invalidAmount = -100L;

        UserPoint currentPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(pointRepository.findById(userId)).thenReturn(currentPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다.");

        // then: 검증 실패로 업데이트 안됨
        verify(pointRepository, times(1)).findById(userId);
        verify(pointRepository, never()).save(anyLong(), anyLong());
        verify(pointRepository, never()).saveHistory(eq(userId), eq(invalidAmount), eq(TransactionType.USE));
    }

    @Test
    @DisplayName("포인트 잔액이 부족할 경우 예외가 발생한다")
    void usePoint_InsufficientBalance() {
        // given
        long userId = 1L;
        long currentAmount = 1000L;
        long useAmount = 1500L;

        UserPoint currentPoint = new UserPoint(userId, currentAmount, System.currentTimeMillis());
        when(pointRepository.findById(userId)).thenReturn(currentPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트가 부족합니다.");

        // then: 검증 실패로 업데이트 안됨
        verify(pointRepository, times(1)).findById(userId);
        verify(pointRepository, never()).save(anyLong(), anyLong());
    }

}