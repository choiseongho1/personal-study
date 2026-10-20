package com.randombox.domain.randombox;

import com.randombox.config.TestRedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestRedisConfig.class)
class RandomBoxInventoryServiceTest {

    @Autowired
    private RandomBoxInventoryService randomBoxInventoryService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RandomBoxRepository randomBoxRepository;

    private static final Long TEST_RANDOM_BOX_ID = 1L;
    private RandomBox testRandomBox;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.execute(connection -> {
                connection.flushDb();
                return null;
            }, true);
        }

        // 테스트용 RandomBox 객체 생성
        testRandomBox = RandomBox.builder()
                .name("테스트 랜덤박스")
                .description("테스트용 랜덤박스입니다.")
                .price(10000)
                .quantity(100)
                .salesStartTime(LocalDateTime.now().minusDays(1))
                .salesEndTime(LocalDateTime.now().plusDays(7))
                .build();

        // RandomBoxRepository mock 설정
        when(randomBoxRepository.findById(TEST_RANDOM_BOX_ID)).thenReturn(Optional.of(testRandomBox));
        when(randomBoxRepository.save(any(RandomBox.class))).thenReturn(testRandomBox);
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 Redis 데이터 초기화
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            redisTemplate.execute(connection -> {
                connection.flushDb();
                return null;
            }, true);
        }
    }

    @Test
    @DisplayName("재고 초기화 성공")
    void initializeInventory_Success() {
        // when
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID);

        // then
        int inventory = randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID);
        assertThat(inventory).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 조회 성공")
    void getInventory_Success() {
        // given
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID);

        // when
        int inventory = randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID);

        // then
        assertThat(inventory).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 감소 성공")
    void decreaseInventory_Success() {
        // given
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID);

        // when
        boolean decreased = randomBoxInventoryService.decreaseInventory(TEST_RANDOM_BOX_ID, 10);

        // then
        assertThat(decreased).isTrue();
        assertThat(randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID)).isEqualTo(90);
        verify(randomBoxRepository, times(1)).save(any(RandomBox.class));
    }

    @Test
    @DisplayName("재고 감소 실패 - 재고 부족")
    void decreaseInventory_Failure_InsufficientInventory() {
        // given
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID);

        // when
        boolean decreased = randomBoxInventoryService.decreaseInventory(TEST_RANDOM_BOX_ID, 200);

        // then
        assertThat(decreased).isFalse();
        assertThat(randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID)).isEqualTo(100);
        verify(randomBoxRepository, never()).save(any(RandomBox.class));
    }

    @Test
    @DisplayName("재고 증가 성공")
    void increaseInventory_Success() {
        // given
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID);
        randomBoxInventoryService.decreaseInventory(TEST_RANDOM_BOX_ID, 50);

        // when
        boolean increased = randomBoxInventoryService.increaseInventory(TEST_RANDOM_BOX_ID, 20);

        // then
        assertThat(increased).isTrue();
        assertThat(randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID)).isEqualTo(70);
        verify(randomBoxRepository, times(3)).save(any(RandomBox.class)); // 초기화 + 감소 + 증가
    }

    @Test
    @DisplayName("재고 동기화 성공")
    void synchronizeInventory_Success() {
        // given
        testRandomBox.updateQuantity(150); // DB의 재고를 150으로 설정
        randomBoxInventoryService.initializeInventory(TEST_RANDOM_BOX_ID); // Redis의 재고를 100으로 초기화

        // when
        randomBoxInventoryService.synchronizeInventory(TEST_RANDOM_BOX_ID);

        // then
        assertThat(randomBoxInventoryService.getInventory(TEST_RANDOM_BOX_ID)).isEqualTo(150);
    }
}
