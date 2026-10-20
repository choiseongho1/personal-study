package com.randombox.api.v1.randombox;

import com.randombox.api.v1.randombox.dto.RandomBoxCreateRequest;
import com.randombox.api.v1.randombox.dto.RandomBoxItemCreateRequest;
import com.randombox.api.v1.randombox.dto.RandomBoxResponse;
import com.randombox.domain.randombox.RandomBox;
import com.randombox.domain.randombox.RandomBoxItem;
import com.randombox.domain.randombox.RandomBoxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/random-boxes")
@RequiredArgsConstructor
public class RandomBoxController {

    private final RandomBoxService randomBoxService;

    @PostMapping
    public ResponseEntity<RandomBoxResponse> createRandomBox(@RequestBody RandomBoxCreateRequest request) {
        RandomBox randomBox = randomBoxService.createRandomBox(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getQuantity(),
                request.getSalesStartTime(),
                request.getSalesEndTime()
        );
        RandomBoxResponse response = RandomBoxResponse.from(randomBox);

        // 새로 생성된 랜덤박스에는 아이템이 없으므로 빈 목록 설정
        response.setItems(Collections.emptyList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RandomBoxResponse> getRandomBox(@PathVariable Long id) {
        RandomBox randomBox = randomBoxService.getRandomBox(id);
        RandomBoxResponse response = RandomBoxResponse.from(randomBox);

        // 랜덤박스 아이템 설정
        List<RandomBoxItem> items = randomBoxService.getRandomBoxItems(id);
        List<RandomBoxResponse.RandomBoxItemResponse> itemResponses = items.stream()
                .map(RandomBoxResponse.RandomBoxItemResponse::from)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RandomBoxResponse>> getAllRandomBoxes() {
        // getAllRandomBoxes 메서드 대신 getRandomBoxesOnSale 메서드 사용
        List<RandomBox> randomBoxes = randomBoxService.getRandomBoxesOnSale();
        List<RandomBoxResponse> responses = randomBoxes.stream()
                .map(randomBox -> {
                    RandomBoxResponse response = RandomBoxResponse.from(randomBox);
                    // 랜덤박스 아이템 설정
                    List<RandomBoxItem> items = randomBoxService.getRandomBoxItems(randomBox.getId());
                    List<RandomBoxResponse.RandomBoxItemResponse> itemResponses = items.stream()
                            .map(RandomBoxResponse.RandomBoxItemResponse::from)
                            .collect(Collectors.toList());
                    response.setItems(itemResponses);
                    return response;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{randomBoxId}/items")
    public ResponseEntity<RandomBoxResponse> addItemToRandomBox(
            @PathVariable Long randomBoxId,
            @RequestBody RandomBoxItemCreateRequest request) {
        // 요청 파라미터 변환
        RandomBoxItem.Rarity rarity = RandomBoxItem.Rarity.valueOf(request.getRarity().toUpperCase());
        BigDecimal probability = BigDecimal.valueOf(request.getProbability());

        // 설명 추가
        String description = request.getName() + " description";

        RandomBoxItem item = randomBoxService.addItemToRandomBox(
                randomBoxId,
                request.getName(),
                description,
                rarity,
                probability
        );

        RandomBox randomBox = randomBoxService.getRandomBox(randomBoxId);
        RandomBoxResponse response = RandomBoxResponse.from(randomBox);

        // 랜덤박스 아이템 설정
        List<RandomBoxItem> items = randomBoxService.getRandomBoxItems(randomBoxId);
        List<RandomBoxResponse.RandomBoxItemResponse> itemResponses = items.stream()
                .map(RandomBoxResponse.RandomBoxItemResponse::from)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRandomBox(@PathVariable Long id) {
        // deleteRandomBox 메서드가 없으므로 updateRandomBox를 사용하여 수량을 0으로 설정
        RandomBox randomBox = randomBoxService.getRandomBox(id);
        randomBoxService.updateRandomBox(
                id,
                randomBox.getName(),
                randomBox.getDescription(),
                randomBox.getPrice(),
                0, // 수량을 0으로 설정하여 사실상 삭제 효과
                randomBox.getSalesStartTime(),
                randomBox.getSalesEndTime()
        );
        return ResponseEntity.noContent().build();
    }
}
