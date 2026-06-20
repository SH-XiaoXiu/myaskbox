package cn.xiuxius.askbox.like.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.xiuxius.askbox.like.entity.LikeCountEntity;
import cn.xiuxius.askbox.like.enums.LikeTargetType;

@Mapper
public interface LikeCountMapper extends BaseMapper<LikeCountEntity> {

    @Insert(
            """
            INSERT INTO like_counts (target_type, target_id, like_count, created_at, updated_at)
            VALUES (#{targetType}, #{targetId}, #{likeCount}, NOW(), NOW())
            ON CONFLICT (target_type, target_id)
            DO UPDATE SET like_count = EXCLUDED.like_count, updated_at = NOW()
            """)
    void upsert(
            @Param("targetType") LikeTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("likeCount") long likeCount);

    @Delete("DELETE FROM like_counts WHERE target_type = #{targetType} AND target_id = #{targetId}")
    void deleteByTarget(@Param("targetType") LikeTargetType targetType, @Param("targetId") Long targetId);
}
