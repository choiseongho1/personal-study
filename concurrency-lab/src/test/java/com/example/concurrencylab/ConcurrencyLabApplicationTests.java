package com.example.concurrencylab;

import com.example.concurrencylab.inventory.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:concurrency_lab_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class ConcurrencyLabApplicationTests {

    @Autowired
    InventoryRepository inventoryRepository;

    @Test
    void flywaySchemaAndSeedAreReadableThroughJpa() {
        var inventory = inventoryRepository.findById(1L).orElseThrow();
        assertThat(inventory.getAvailable()).isEqualTo(100);
        assertThat(inventory.getUnitPrice()).isEqualByComparingTo("10000.00");
    }
}
