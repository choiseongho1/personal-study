package com.example.concurrencylab.common;

public class ExerciseNotImplementedException extends RuntimeException {
    public ExerciseNotImplementedException(String exercise) {
        super("직접 구현할 실습입니다: " + exercise);
    }
}
