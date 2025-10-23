package io.hhplus.tdd.point;
import io.hhplus.tdd.point.validator.PointValidator;
import io.hhplus.tdd.repository.PointRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    private final PointRepository pointRepository;
    private final PointValidator pointValidator;

    private final ConcurrentHashMap<Long, ReentrantLock> userLock = new ConcurrentHashMap<>();

    public PointService(PointRepository pointRepository , PointValidator pointValidator) {
        this.pointRepository = pointRepository;
        this.pointValidator = pointValidator;
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
        ReentrantLock lock = userLock.computeIfAbsent(id, k -> new ReentrantLock());

        lock.lock();

        try {
            // 조회
            UserPoint userPoint = pointRepository.findById(id);
            List<PointHistory> history = pointRepository.findHistoryByUserId(id);

            // 검증
            pointValidator.validateCharge(userPoint.point(), amount, history);

            // 업데이트
            UserPoint updatedPoint = pointRepository.save(id, userPoint.point() + amount);

            // 히스토리 적재
            pointRepository.saveHistory(id, amount, TransactionType.CHARGE);

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 포인트 사용
     */
    public UserPoint usePoint(long id, long amount) {
        ReentrantLock lock = userLock.computeIfAbsent(id, k -> new ReentrantLock());

        lock.lock();
        try {
            // 조회
            UserPoint userPoint = pointRepository.findById(id);

            // 검증
            pointValidator.validateUse(userPoint.point(), amount);

            // 업데이트
            UserPoint updatedPoint = pointRepository.save(id, userPoint.point() - amount);

            // 히스토리 적재
            pointRepository.saveHistory(id, amount, TransactionType.USE);

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }

}