package com.movie.api.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bounded IN queries, deduplicated IDs, and no invalid empty IN clause. */
public final class BatchLookup {
    private static final int BATCH_SIZE = 500;

    private BatchLookup() {
    }

    public static <T> Map<String, T> byId(Collection<String> ids, BaseMapper<T> mapper, Function<T, String> id) {
        List<String> distinct = new ArrayList<>(ids.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<String, T> result = new HashMap<>();
        for (int start = 0; start < distinct.size(); start += BATCH_SIZE) {
            for (T row : mapper.selectBatchIds(distinct.subList(start, Math.min(start + BATCH_SIZE, distinct.size())))) {
                result.put(id.apply(row), row);
            }
        }
        return result;
    }
}
