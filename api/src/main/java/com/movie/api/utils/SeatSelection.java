package com.movie.api.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Keeps the existing "1号2号" wire/database representation. */
public final class SeatSelection {
    private SeatSelection() {
    }

    public static List<Integer> parse(String value) {
        if (value == null || !value.matches("[0-9]+(?:号[0-9]+)*号?")) {
            throw new IllegalArgumentException("请选择有效的座位");
        }
        Set<Integer> seats = new LinkedHashSet<>();
        for (String token : value.split("号")) {
            final int seat;
            try {
                seat = Integer.parseInt(token);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("座位编号无效");
            }
            if (seat <= 0 || !seats.add(seat)) {
                throw new IllegalArgumentException("座位编号无效或重复");
            }
        }
        return new ArrayList<>(seats);
    }

    public static List<Integer> validate(String value, Integer capacity) {
        List<Integer> seats = parse(value);
        if (capacity == null || capacity <= 0 || seats.stream().anyMatch(seat -> seat > capacity)) {
            throw new IllegalArgumentException("所选座位超出场次座位范围");
        }
        return seats;
    }
}
