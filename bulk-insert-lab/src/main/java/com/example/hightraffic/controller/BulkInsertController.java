package com.example.hightraffic.controller;

import com.example.hightraffic.service.BulkInsertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bulk")
public class BulkInsertController {
    private final BulkInsertService bulkInsertService;

    @PostMapping("/insert/jdbc")
    public String insertJdbc(@RequestParam(defaultValue = "1000000") int total) throws SQLException {
        long start = System.currentTimeMillis();
        bulkInsertService.insertUsersJdbc(total);
        long end = System.currentTimeMillis();
        return "[JDBC] Inserted " + total + " users in " + (end - start)  + "ms";
    }

    @PostMapping("/insert/jpa")
    public String insertJpa(@RequestParam(defaultValue = "1000000") int total) {
        long start = System.currentTimeMillis();
        bulkInsertService.insertUsersJpa(total);
        long end = System.currentTimeMillis();
        return "[JPA] Inserted " + total + " users in " + (end - start) + "ms";
    }
}
