package com.study.global.util;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Function;

/**
 * QueryDSL 조건식 유틸리티 클래스
 *
 * 동적 쿼리 작성 시 자주 사용되는 조건식을 편리하게 사용할 수 있도록 지원
 */
public class QueryUtils {

    /**
     * 문자열 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression eq(String value, StringPath field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * 숫자 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static <T extends Number & Comparable<?>> BooleanExpression eq(T value, NumberPath<T> field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * Boolean 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression eq(Boolean value, BooleanPath field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * LocalDateTime 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression eq(LocalDateTime value, DateTimePath<LocalDateTime> field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * LocalDate 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression eq(LocalDate value, DatePath<LocalDate> field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * Enum 필드에 대한 equals 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static <T extends Enum<T>> BooleanExpression eq(T value, EnumPath<T> field) {
        return value != null ? field.eq(value) : null;
    }

    /**
     * 문자열 필드에 대한 contains 조건 (null 안전)
     *
     * @param value 포함 여부를 확인할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression contains(String value, StringPath field) {
        return value != null ? field.contains(value) : null;
    }

    /**
     * 문자열 필드에 대한 startsWith 조건 (null 안전)
     *
     * @param value 시작 문자열
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression startsWith(String value, StringPath field) {
        return value != null ? field.startsWith(value) : null;
    }

    /**
     * 문자열 필드에 대한 endsWith 조건 (null 안전)
     *
     * @param value 끝 문자열
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression endsWith(String value, StringPath field) {
        return value != null ? field.endsWith(value) : null;
    }

    /**
     * 문자열 필드에 대한 대소문자 무시 contains 조건 (null 안전)
     *
     * @param value 포함 여부를 확인할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression containsIgnoreCase(String value, StringPath field) {
        return value != null ? field.containsIgnoreCase(value) : null;
    }

    /**
     * 숫자 필드에 대한 크거나 같음(>=) 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static <T extends Number & Comparable<?>> BooleanExpression goe(T value, NumberPath<T> field) {
        return value != null ? field.goe(value) : null;
    }

    /**
     * 숫자 필드에 대한 작거나 같음(<=) 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static <T extends Number & Comparable<?>> BooleanExpression loe(T value, NumberPath<T> field) {
        return value != null ? field.loe(value) : null;
    }

    /**
     * LocalDateTime 필드에 대한 크거나 같음(>=) 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression goe(LocalDateTime value, DateTimePath<LocalDateTime> field) {
        return value != null ? field.goe(value) : null;
    }

    /**
     * LocalDateTime 필드에 대한 작거나 같음(<=) 조건 (null 안전)
     *
     * @param value 비교할 값
     * @param field 필드 표현식
     * @return 조건식 (값이 null이면 null 반환)
     */
    public static BooleanExpression loe(LocalDateTime value, DateTimePath<LocalDateTime> field) {
        return value != null ? field.loe(value) : null;
    }

    /**
     * 컬렉션에 포함 여부 조건 (in) (null 안전)
     *
     * @param values 값 컬렉션
     * @param field 필드 표현식
     * @return 조건식 (값이 null이거나 비어있으면 null 반환)
     */
    public static <T> BooleanExpression in(Collection<T> values, SimpleExpression<T> field) {
        return values != null && !values.isEmpty() ? field.in(values) : null;
    }

    /**
     * Between 조건 (null 안전)
     *
     * @param from 시작 값
     * @param to 종료 값
     * @param field 필드 표현식
     * @return 조건식 (from 또는 to가 null이면 null 반환)
     */
    public static <T extends Comparable<?>> BooleanExpression between(T from, T to, ComparableExpression<T> field) {
        return from != null && to != null ? field.between(from, to) : null;
    }

    /**
     * LocalDateTime 필드에 대한 Between 조건 (null 안전)
     *
     * @param from 시작 시간
     * @param to 종료 시간
     * @param field 필드 표현식
     * @return 조건식 (from 또는 to가 null이면 null 반환)
     */
    public static BooleanExpression between(LocalDateTime from, LocalDateTime to, DateTimePath<LocalDateTime> field) {
        return from != null && to != null ? field.between(from, to) : null;
    }

    /**
     * LocalDate 필드에 대한 Between 조건 (null 안전)
     *
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @param field 필드 표현식
     * @return 조건식 (from 또는 to가 null이면 null 반환)
     */
    public static BooleanExpression between(LocalDate from, LocalDate to, DatePath<LocalDate> field) {
        return from != null && to != null ? field.between(from, to) : null;
    }

    /**
     * 여러 조건식을 AND 연산으로 결합 (null 안전)
     * null인 조건은 무시됨
     *
     * @param predicates 조건식 배열
     * @return 결합된 조건식
     */
    public static BooleanBuilder and(Predicate... predicates) {
        BooleanBuilder builder = new BooleanBuilder();

        for (Predicate predicate : predicates) {
            if (predicate != null) {
                builder.and(predicate);
            }
        }

        return builder;
    }

    /**
     * 여러 조건식을 OR 연산으로 결합 (null 안전)
     * null인 조건은 무시됨
     *
     * @param predicates 조건식 배열
     * @return 결합된 조건식
     */
    public static BooleanBuilder or(Predicate... predicates) {
        BooleanBuilder builder = new BooleanBuilder();

        boolean first = true;
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                if (first) {
                    builder.and(predicate);
                    first = false;
                } else {
                    builder.or(predicate);
                }
            }
        }

        return builder;
    }

    /**
     * 동적 정렬 조건 생성
     *
     * @param property 정렬 속성명
     * @param direction 정렬 방향 ("asc" 또는 "desc")
     * @param paths 정렬 가능한 경로 맵
     * @return 정렬 표현식
     */
    public static <T> OrderSpecifier<?> getOrderSpecifier(String property, String direction,
                                                          Function<String, Expression<?>> pathResolver) {
        Expression<?> path = pathResolver.apply(property);

        if (path == null) {
            throw new IllegalArgumentException("Unknown sort property: " + property);
        }

        Order order = "desc".equalsIgnoreCase(direction) ? Order.DESC : Order.ASC;

        return new OrderSpecifier(order, path);
    }
}
