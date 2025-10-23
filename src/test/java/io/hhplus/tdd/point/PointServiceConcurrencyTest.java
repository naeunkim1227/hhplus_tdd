package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.*;


import java.util.concurrent.*;

/**
 * PointController 통합 테스트
 * Given-When-Then 스타일로 작성해 보았습니다.
 */
@SpringBootTest
@DisplayName("포인트 동시성 테스트")
public class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    private long testUserId;
    private int threadCount = 60;
    private long chargeAmount = 1000L;
    private long useAmount = 500L;


    @BeforeEach
    void setUp() {
        testUserId = System.currentTimeMillis();
    }

    @Test
    @DisplayName("n개의 가상스레드가 동시에 포인트 충전")
    void chargePoint_concurrency_NThreads() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        //동시 종료
        CountDownLatch latch = new CountDownLatch(threadCount);

        StopWatch stopWatch = new StopWatch("포인트 충전 시간 측정 시작");
        stopWatch.start();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(testUserId, chargeAmount);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        stopWatch.stop();

        // then
        UserPoint result = pointService.getPoint(testUserId);

        System.out.println("기대 금액 : " + threadCount * chargeAmount);
        System.out.println("실제 금액 : " + result.point());
        assertThat(result.point()).isEqualTo(threadCount * chargeAmount);

        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimePerThread = (double) totalTimeMs / threadCount;
        System.out.println("============ 응답 시간 결과 ============");
        System.out.println("총 실행 시간(ms): " + totalTimeMs);
        System.out.println("스레드당 평균 처리 시간(ms): " + avgTimePerThread);
        System.out.println("기대 금액 : " + (threadCount * chargeAmount));
        System.out.println("실제 금액 : " + result.point());
        System.out.println("=====================================");
    }


    @Test
    @DisplayName("n개의 가상스레드가 동시에 포인트 사용")
    void usePoint_concurrency_NThreads() throws Exception {
        //Given
        long chargePoint = 200000;
        long expectedPoint = chargePoint - (useAmount * threadCount);

        pointService.chargePoint(testUserId, chargePoint);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //응답 시간 측정
        StopWatch stopWatch = new StopWatch("포인트 충전 시간 측정 시작");
        stopWatch.start();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(testUserId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        stopWatch.stop();

        // then
        UserPoint result = pointService.getPoint(testUserId);
        assertThat(result.point()).isEqualTo(expectedPoint);

        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimePerThread = (double) totalTimeMs / threadCount;
        System.out.println("============ 응답 시간 결과 ============");
        System.out.println("총 실행 시간(ms): " + totalTimeMs);
        System.out.println("스레드당 평균 처리 시간(ms): " + avgTimePerThread);
        System.out.println("기대 금액 : " + (threadCount * chargeAmount));
        System.out.println("실제 금액 : " + result.point());
        System.out.println("=====================================");
    }

}
