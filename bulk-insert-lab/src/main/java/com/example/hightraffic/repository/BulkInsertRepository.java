package com.example.hightraffic.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.example.hightraffic.entity.UserData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BulkInsertRepository {
    private final DataSource dataSource;

    public void bulkInsert(List<UserData> users) throws SQLException {
        String sql = "INSERT INTO user_data (name, email, age) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int batchSize = 10_000;
            int count = 0;

            for (UserData user : users) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setInt(3, user.getAge());
                ps.addBatch();

                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
