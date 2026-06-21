package cn.xiuxius.askbox.ai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.xiuxius.askbox.ai.entity.AiReviewEntity;

@Mapper
public interface AiReviewMapper extends BaseMapper<AiReviewEntity> {
    List<PopularAiReviewExample> selectPopularExamples(@Param("boxUserId") Long boxUserId, @Param("limit") int limit);

    record PopularAiReviewExample(Long aiReviewId, Long questionId, Long boxUserId, String content, Long likeCount) {}
}
