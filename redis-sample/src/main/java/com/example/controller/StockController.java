package com.example.controller;

import com.example.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final RedisTemplate<String, String> redisTemplate;

    @PostMapping("/init")
    public String initStock() {
        redisTemplate.opsForValue().set("product:stock", "10");
        return "재고 초기화 완료";
    }

    @PostMapping("/decrease")
    public String decrease() {
        stockService.decreaseStock("1");
        return "재고 감소 완료";
    }

    @PostMapping("/decreaseNotLock")
    public String decreaseNotLock() {
        stockService.decreaseStockDoNotLock("1");
        return "락 없이 재고 감소 완료";
    }

    @PostMapping("/decreaseLua")
    public String decreaseWithLua() {
        Long result = stockService.decreaseStockWithLua("1");

        return "Lua 실행 결과: " + result;
    }

    @PostMapping("/decreaseOptimistic")
    public String decreaseOptimistic() {
        boolean success = stockService.decreaseStockWithWatch("1");
        return success ? "감소 성공" : "재고 없음";
    }
}
