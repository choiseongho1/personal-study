package com.randombox.domain.randombox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomBoxServiceTest {

    @Mock
    private RandomBoxRepository randomBoxRepository;

    @Mock
    private RandomBoxItemRepository randomBoxItemRepository;

    @InjectMocks
    private RandomBoxService randomBoxService;

    private RandomBox randomBox;
    private RandomBoxItem item1;
    private RandomBoxItem item2;
    private LocalDateTime now;
    private LocalDateTime future;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        future = now.plusDays(7);

        randomBox = RandomBox.builder()
                .name("테스트 랜덤박스")
                .description("테스트용 랜덤박스입니다.")
                .price(1000)
                .quantity(100)
                .salesStartTime(now.minusDays(1))
                .salesEndTime(future)
                .build();

        // ID 필드 설정
        ReflectionTestUtils.setField(randomBox, "id", 1L);

        item1 = RandomBoxItem.builder()
                .randomBox(randomBox)
                .name("일반 아이템")
                .description("흔한 아이템입니다.")
                .rarity(RandomBoxItem.Rarity.COMMON)
                .probability(new BigDecimal("70.0"))
                .build();

        item2 = RandomBoxItem.builder()
                .randomBox(randomBox)
                .name("레어 아이템")
                .description("희귀한 아이템입니다.")
                .rarity(RandomBoxItem.Rarity.RARE)
                .probability(new BigDecimal("30.0"))
                .build();

        // ID 필드 설정
        ReflectionTestUtils.setField(item1, "id", 1L);
        ReflectionTestUtils.setField(item2, "id", 2L);
    }

    @Test
    @DisplayName("랜덤박스 생성 성공")
    void createRandomBox_Success() {
        // given
        when(randomBoxRepository.save(any(RandomBox.class))).thenReturn(randomBox);

        // when
        RandomBox result = randomBoxService.createRandomBox(
                "테스트 랜덤박스",
                "테스트용 랜덤박스입니다.",
                1000,
                100,
                now.minusDays(1),
                future
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 랜덤박스");
        assertThat(result.getDescription()).isEqualTo("테스트용 랜덤박스입니다.");
        assertThat(result.getPrice()).isEqualTo(1000);
        assertThat(result.getQuantity()).isEqualTo(100);
        verify(randomBoxRepository, times(1)).save(any(RandomBox.class));
    }

    @Test
    @DisplayName("랜덤박스 생성 실패 - 잘못된 판매 기간")
    void createRandomBox_Failure_InvalidSalesPeriod() {
        // given
        LocalDateTime invalidEndTime = now.minusDays(2);

        // when & then
        assertThatThrownBy(() -> randomBoxService.createRandomBox(
                "테스트 랜덤박스",
                "테스트용 랜덤박스입니다.",
                1000,
                100,
                now.minusDays(1),
                invalidEndTime
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("판매 종료 시간은 판매 시작 시간 이후여야 합니다.");
    }

    @Test
    @DisplayName("랜덤박스 조회 성공")
    void getRandomBox_Success() {
        // given
        when(randomBoxRepository.findById(1L)).thenReturn(Optional.of(randomBox));

        // when
        RandomBox result = randomBoxService.getRandomBox(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 랜덤박스");
    }

    @Test
    @DisplayName("랜덤박스 조회 실패 - 존재하지 않는 랜덤박스")
    void getRandomBox_Failure_NotFound() {
        // given
        when(randomBoxRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> randomBoxService.getRandomBox(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 랜덤박스입니다.");
    }

    @Test
    @DisplayName("판매 중인 랜덤박스 목록 조회")
    void getRandomBoxesOnSale_Success() {
        // given
        List<RandomBox> randomBoxes = Arrays.asList(randomBox);
        when(randomBoxRepository.findAllOnSale(any(LocalDateTime.class))).thenReturn(randomBoxes);

        // when
        List<RandomBox> result = randomBoxService.getRandomBoxesOnSale();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 랜덤박스");
    }

    @Test
    @DisplayName("랜덤박스 아이템 추가 성공")
    void addItemToRandomBox_Success() {
        // given
        when(randomBoxRepository.findById(1L)).thenReturn(Optional.of(randomBox));
        when(randomBoxItemRepository.findByRandomBoxId(1L)).thenReturn(Arrays.asList());
        when(randomBoxItemRepository.save(any(RandomBoxItem.class))).thenReturn(item1);

        // when
        RandomBoxItem result = randomBoxService.addItemToRandomBox(
                1L,
                "일반 아이템",
                "흔한 아이템입니다.",
                RandomBoxItem.Rarity.COMMON,
                new BigDecimal("70.0")
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("일반 아이템");
        assertThat(result.getRarity()).isEqualTo(RandomBoxItem.Rarity.COMMON);
        assertThat(result.getProbability()).isEqualTo(new BigDecimal("70.0"));
    }

    @Test
    @DisplayName("랜덤박스 아이템 추가 실패 - 확률 합 초과")
    void addItemToRandomBox_Failure_ProbabilityExceeded() {
        // given
        when(randomBoxRepository.findById(1L)).thenReturn(Optional.of(randomBox));
        when(randomBoxItemRepository.findByRandomBoxId(1L)).thenReturn(Arrays.asList(item1, item2));

        // when & then
        assertThatThrownBy(() -> randomBoxService.addItemToRandomBox(
                1L,
                "에픽 아이템",
                "매우 희귀한 아이템입니다.",
                RandomBoxItem.Rarity.EPIC,
                new BigDecimal("10.0")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모든 아이템의 확률 합은 100을 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("랜덤 아이템 추첨 성공")
    void drawRandomItem_Success() {
        // given
        when(randomBoxItemRepository.findByRandomBoxId(1L)).thenReturn(Arrays.asList(item1, item2));

        // when
        RandomBoxItem result = randomBoxService.drawRandomItem(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRandomBox()).isEqualTo(randomBox);
        assertThat(result.getRarity()).isIn(RandomBoxItem.Rarity.COMMON, RandomBoxItem.Rarity.RARE);
    }

    @Test
    @DisplayName("랜덤 아이템 추첨 실패 - 아이템 없음")
    void drawRandomItem_Failure_NoItems() {
        // given
        when(randomBoxItemRepository.findByRandomBoxId(1L)).thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> randomBoxService.drawRandomItem(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("랜덤박스에 아이템이 없습니다.");
    }
}
