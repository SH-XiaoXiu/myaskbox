package cn.xiuxius.askbox.like.mapper;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.xiuxius.askbox.like.entity.LikeCountEntity;
import cn.xiuxius.askbox.like.enums.LikeTargetType;

@Mapper
public interface LikeCountMapper extends BaseMapper<LikeCountEntity> {
    void upsert(
            @Param("targetType") LikeTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("likeCount") long likeCount,
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("updatedAt") OffsetDateTime updatedAt);
}
