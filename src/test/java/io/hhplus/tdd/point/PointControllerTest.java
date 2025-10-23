package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PointController 통합 테스트
 * Given-When-Then 스타일로 작성해 보았습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PointController 통합 테스트 - Given-When-Then")
class PointControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    public PointControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @DisplayName("유저의 포인트를 조회할 수 있다")
    void getPoint_Success() throws Exception {
        // Given: 포인트를 먼저 충전한 상태
        long testUserId = 1L;
        long chargeAmount = 1000L;
        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        // When
        mockMvc.perform(get("/point/{id}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))

        // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트를 충전하고 포인트의 히스토리를 조회할 수 있다")
    void chargePoint_Success() throws Exception {
        // Given: 포인트 충전 및 사용 이력 생성
        long testUserId = 2L;
        long chargeAmount = 1000L;

        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        // When
        mockMvc.perform(get("/point/{id}/histories", testUserId)
                .contentType(MediaType.APPLICATION_JSON))

        // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(chargeAmount));
    }

    @Test
    @DisplayName("여러 번 충전할 수 있다")
    void chargePoint_Accumulation() throws Exception {
        // Given
        long testUserId = 3L;
        long firstCharge = 1000L;
        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(firstCharge)));

        // When
        long secondCharge = 2000L;
        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(secondCharge)))

        // Then: 포인트 누적
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge + secondCharge));
    }

    @Test
    @DisplayName("포인트를 충전후 사용할 수 있다")
    void usePoint_Success() throws Exception {
        // Given
        long testUserId = 4L;
        long chargeAmount = 5000L;
        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargeAmount)));

        // When
        long useAmount = 2000L;
        mockMvc.perform(patch("/point/{id}/use", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(useAmount)))

        // Then: 사용 후 잔액 확인
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId))
                .andExpect(jsonPath("$.point").value(chargeAmount - useAmount));
    }




    @Test
    @DisplayName("일일 최대 충전 금액 이상 충전할 수 없다")
    void chargePoint_MaxPoint() throws Exception {
        //Given
        long testUserId = 5L;
        long maxAmount =5_000_001L;

        //When
        mockMvc.perform(patch("/point/{id}/charge", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(maxAmount)))

        //Then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message",
                        containsString("일일 충전 한도(오백만원)를 초과했습니다.")));

    }






}