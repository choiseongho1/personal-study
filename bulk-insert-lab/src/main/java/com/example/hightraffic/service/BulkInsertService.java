package com.example.hightraffic.service;

import com.example.hightraffic.entity.UserData;
import com.example.hightraffic.repository.BulkInsertRepository;
import com.example.hightraffic.repository.UserDataJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class BulkInsertService {
    private final BulkInsertRepository bulkInsertRepository;
    private final UserDataJpaRepository userDataJpaRepository;

    public void insertUsersJdbc(int total) throws SQLException {
        int chunkSize = 100_000;
        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<UserData> chunk = IntStream.range(i, end)
                .mapToObj(j -> new UserData(null, "user" + j, "user" + j + "@mail.com", (int)(Math.random() * 100)))
                .toList();
            bulkInsertRepository.bulkInsert(chunk);
            System.out.println("Inserted chunk (JDBC): " + end);
        }
    }

    public void insertUsersJpa(int total) {
        int chunkSize = 10_000;
        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<UserData> chunk = IntStream.range(i, end)
                .mapToObj(j -> new UserData(null, "user" + j, "user" + j + "@mail.com", (int)(Math.random() * 100)))
                .toList();
            userDataJpaRepository.saveAll(chunk);
            System.out.println("Inserted chunk (JPA): " + end);
            System.out.println("111");
        }
    }
}
