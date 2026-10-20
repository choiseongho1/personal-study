package com.randombox.domain.randombox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RandomBoxService {

    private final RandomBoxRepository randomBoxRepository;
    private final RandomBoxItemRepository randomBoxItemRepository;
    private final Random random = new Random();

    @Transactional
    public RandomBox createRandomBox(String name, String description, Integer price, Integer quantity,
                                    LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        validateRandomBoxTimes(salesStartTime, salesEndTime);

        RandomBox randomBox = RandomBox.builder()
                .name(name)
                .description(description)
                .price(price)
                .quantity(quantity)
                .salesStartTime(salesStartTime)
                .salesEndTime(salesEndTime)
                .build();

        return randomBoxRepository.save(randomBox);
    }

    private void validateRandomBoxTimes(LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        if (salesEndTime.isBefore(salesStartTime)) {
            throw new IllegalArgumentException("판매 종료 시간은 판매 시작 시간 이후여야 합니다.");
        }
    }

    public RandomBox getRandomBox(Long randomBoxId) {
        return randomBoxRepository.findById(randomBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));
    }

    public List<RandomBox> getRandomBoxesOnSale() {
        return randomBoxRepository.findAllOnSale(LocalDateTime.now());
    }

    public List<RandomBox> searchRandomBoxes(String keyword) {
        return randomBoxRepository.findByNameContaining(keyword);
    }

    @Transactional
    public RandomBox updateRandomBox(Long randomBoxId, String name, String description, Integer price,
                                    Integer quantity, LocalDateTime salesStartTime, LocalDateTime salesEndTime) {
        validateRandomBoxTimes(salesStartTime, salesEndTime);

        RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

        randomBox.update(name, description, price, quantity, salesStartTime, salesEndTime);
        return randomBox;
    }

    @Transactional
    public RandomBoxItem addItemToRandomBox(Long randomBoxId, String name, String description,
                                          RandomBoxItem.Rarity rarity, BigDecimal probability) {
        RandomBox randomBox = randomBoxRepository.findById(randomBoxId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랜덤박스입니다."));

        validateProbability(probability);
        validateTotalProbability(randomBoxId, probability);

        RandomBoxItem item = RandomBoxItem.builder()
                .randomBox(randomBox)
                .name(name)
                .description(description)
                .rarity(rarity)
                .probability(probability)
                .build();

        return randomBoxItemRepository.save(item);
    }

    private void validateProbability(BigDecimal probability) {
        if (probability.compareTo(BigDecimal.ZERO) <= 0 || probability.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("확률은 0보다 크고 100 이하여야 합니다.");
        }
    }

    private void validateTotalProbability(Long randomBoxId, BigDecimal newProbability) {
        List<RandomBoxItem> existingItems = randomBoxItemRepository.findByRandomBoxId(randomBoxId);
        BigDecimal totalProbability = existingItems.stream()
                .map(RandomBoxItem::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalProbability.add(newProbability).compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("모든 아이템의 확률 합은 100을 초과할 수 없습니다.");
        }
    }

    public List<RandomBoxItem> getRandomBoxItems(Long randomBoxId) {
        return randomBoxItemRepository.findByRandomBoxId(randomBoxId);
    }

    @Transactional
    public RandomBoxItem updateRandomBoxItem(Long itemId, String name, String description,
                                           RandomBoxItem.Rarity rarity, BigDecimal probability) {
        RandomBoxItem item = randomBoxItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));

        validateProbability(probability);

        // 기존 확률을 제외하고 새 확률로 검증
        List<RandomBoxItem> existingItems = randomBoxItemRepository.findByRandomBoxId(item.getRandomBox().getId());
        BigDecimal totalProbability = existingItems.stream()
                .filter(i -> !i.getId().equals(itemId))
                .map(RandomBoxItem::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalProbability.add(probability).compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("모든 아이템의 확률 합은 100을 초과할 수 없습니다.");
        }

        item.update(name, description, rarity, probability);
        return item;
    }

    public RandomBoxItem drawRandomItem(Long randomBoxId) {
        List<RandomBoxItem> items = randomBoxItemRepository.findByRandomBoxId(randomBoxId);
        if (items.isEmpty()) {
            throw new IllegalStateException("랜덤박스에 아이템이 없습니다.");
        }

        double randomValue = random.nextDouble() * 100;
        double cumulativeProbability = 0.0;

        for (RandomBoxItem item : items) {
            cumulativeProbability += item.getProbability().doubleValue();
            if (randomValue <= cumulativeProbability) {
                return item;
            }
        }

        // 부동소수점 오차로 인해 마지막 아이템이 선택되지 않을 수 있으므로 마지막 아이템 반환
        return items.get(items.size() - 1);
    }
}
