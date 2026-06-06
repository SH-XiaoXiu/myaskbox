package cn.xiuxius.askbox.common;

import java.util.List;
import java.util.function.Function;

import com.baomidou.mybatisplus.core.metadata.IPage;

public record PageResult<T>(List<T> records, long total, long page, long pageSize, long totalPages) {

    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(records.stream().map(mapper).toList(), total, page, pageSize, totalPages);
    }
}
