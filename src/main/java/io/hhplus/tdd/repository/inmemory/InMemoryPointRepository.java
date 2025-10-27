package io.hhplus.tdd.repository.inmemory;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.repository.PointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryPointRepository implements PointRepository {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public
    InMemoryPointRepository(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    @Override
    public UserPoint findById(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint save(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

    @Override
    public List<PointHistory> findHistoryByUserId(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    @Override
    public PointHistory saveHistory(long id, long amount, TransactionType type) {
        return pointHistoryTable.insert(id, amount, type, System.currentTimeMillis());
    }
}
