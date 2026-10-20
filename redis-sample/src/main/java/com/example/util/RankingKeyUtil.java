package com.example.util;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class RankingKeyUtil {

    public static String getWeeklyRankingKey() {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int weekNumber = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.getYear();
        return String.format("post:ranking:%d-W%d", year, weekNumber);
    }
}
