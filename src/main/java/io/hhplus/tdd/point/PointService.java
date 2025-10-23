package io.hhplus.tdd.point;

import io.hhplus.tdd.repository.PointRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final PointRepository pointRepository;

    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    /**
     * 포인트 조회
     */
    public UserPoint getPoint(long id) {
        //조회
        return pointRepository.findById(id);
    }

    /**
     * 포인트 충전/이용 내역을 조회
     */
    public List<PointHistory> getHistory(long id) {
        //조회
        return pointRepository.findHistoryByUserId(id);
    }

    /**
     * 포인트 충전
     */
    public UserPoint chargePoint(long id, long amount) {
        // 조회
        UserPoint userPoint = pointRepository.findById(id);

        // 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 업데이트
        UserPoint updatedPoint = pointRepository.save(id, userPoint.point() + amount);

        // 히스토리 적재
        pointRepository.saveHistory(id, amount, TransactionType.CHARGE);

        return updatedPoint;
    }

    /**
     * 포인트 사용
     */
    public UserPoint usePoint(long id, long amount) {
        // 조회
        UserPoint userPoint = pointRepository.findById(id);

        // 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        if(amount > userPoint.point()) {
            throw new IllegalArgumentException( "포인트가 부족합니다.");
        }

        // 업데이트
        UserPoint updatedPoint = pointRepository.save(id, userPoint.point() - amount);

        // 히스토리 적재
        pointRepository.saveHistory(id, amount, TransactionType.USE);

        return updatedPoint;
    }
}