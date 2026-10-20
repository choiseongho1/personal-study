package com.randombox.domain.queue;

import com.randombox.config.TestRedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestRedisConfig.class)
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final Long TEST_RANDOM_BOX_ID = 1L;
    private static final Long TEST_USER_ID_1 = 101L;
    private static final Long TEST_USER_ID_2 = 102L;
    private static final Long TEST_USER_ID_3 = 103L;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 Redis 데이터 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("대기열에 사용자 추가 성공")
    void addToQueue_Success() {
        // when
        int position = queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);

        // then
        assertThat(position).isEqualTo(0); // 첫 번째 사용자는 위치 0
        assertThat(queueService.getWaitingCount(TEST_RANDOM_BOX_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("대기열에 여러 사용자 추가 성공")
    void addMultipleUsersToQueue_Success() {
        // when
        int position1 = queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        int position2 = queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        int position3 = queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // then
        assertThat(position1).isEqualTo(0); // 첫 번째 사용자는 위치 0
        assertThat(position2).isEqualTo(1); // 두 번째 사용자는 위치 1
        assertThat(position3).isEqualTo(2); // 세 번째 사용자는 위치 2
        assertThat(queueService.getWaitingCount(TEST_RANDOM_BOX_ID)).isEqualTo(3);
    }

    @Test
    @DisplayName("대기열에서 사용자 위치 조회 성공")
    void getPosition_Success() {
        // given
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // when
        int position1 = queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        int position2 = queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        int position3 = queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // then
        assertThat(position1).isEqualTo(0);
        assertThat(position2).isEqualTo(1);
        assertThat(position3).isEqualTo(2);
    }

    @Test
    @DisplayName("대기열에서 다음 사용자 가져오기 성공")
    void getNextUser_Success() {
        // given
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // when
        Long nextUser1 = queueService.getNextUser(TEST_RANDOM_BOX_ID);
        Long nextUser2 = queueService.getNextUser(TEST_RANDOM_BOX_ID);
        Long nextUser3 = queueService.getNextUser(TEST_RANDOM_BOX_ID);
        Long nextUser4 = queueService.getNextUser(TEST_RANDOM_BOX_ID);

        // then
        assertThat(nextUser1).isEqualTo(TEST_USER_ID_1);
        assertThat(nextUser2).isEqualTo(TEST_USER_ID_2);
        assertThat(nextUser3).isEqualTo(TEST_USER_ID_3);
        assertThat(nextUser4).isNull(); // 대기열이 비어있으면 null 반환
        assertThat(queueService.getWaitingCount(TEST_RANDOM_BOX_ID)).isEqualTo(0);
    }

    @Test
    @DisplayName("대기열에서 사용자 제거 성공")
    void removeFromQueue_Success() {
        // given
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // when
        boolean removed = queueService.removeFromQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);

        // then
        assertThat(removed).isTrue();
        assertThat(queueService.getWaitingCount(TEST_RANDOM_BOX_ID)).isEqualTo(2);
        assertThat(queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_1)).isEqualTo(0);
        assertThat(queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_2)).isEqualTo(-1); // 제거된 사용자는 -1
        assertThat(queueService.getPosition(TEST_RANDOM_BOX_ID, TEST_USER_ID_3)).isEqualTo(1); // 위치가 한 칸 앞으로 이동
    }

    @Test
    @DisplayName("예상 대기 시간 계산 성공")
    void getEstimatedWaitTime_Success() {
        // given
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        queueService.addToQueue(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // when
        int waitTime1 = queueService.getEstimatedWaitTime(TEST_RANDOM_BOX_ID, TEST_USER_ID_1);
        int waitTime2 = queueService.getEstimatedWaitTime(TEST_RANDOM_BOX_ID, TEST_USER_ID_2);
        int waitTime3 = queueService.getEstimatedWaitTime(TEST_RANDOM_BOX_ID, TEST_USER_ID_3);

        // then
        assertThat(waitTime1).isEqualTo(0); // 첫 번째 사용자는 대기 시간 0
        assertThat(waitTime2).isEqualTo(30); // 두 번째 사용자는 대기 시간 30초
        assertThat(waitTime3).isEqualTo(60); // 세 번째 사용자는 대기 시간 60초
    }
}
