package io.hhplus.tdd.repository;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;

public interface PointRepository {
    UserPoint findById(long id);
    UserPoint save(long id, long amount);

    List<PointHistory> findHistoryByUserId(long userId);
    PointHistory saveHistory(long userId, long amount, TransactionType type);
}
