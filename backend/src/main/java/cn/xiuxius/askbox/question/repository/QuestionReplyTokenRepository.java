package cn.xiuxius.askbox.question.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import cn.xiuxius.askbox.question.entity.QuestionReplyTokenEntity;
import cn.xiuxius.askbox.question.mapper.QuestionReplyTokenMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuestionReplyTokenRepository {
    private final QuestionReplyTokenMapper mapper;

    public void insert(QuestionReplyTokenEntity token) {
        mapper.insert(token);
    }

    public QuestionReplyTokenEntity findByTokenHash(String tokenHash) {
        return mapper.selectOne(new LambdaQueryWrapper<QuestionReplyTokenEntity>()
                .eq(QuestionReplyTokenEntity::getTokenHash, tokenHash));
    }

    public void update(QuestionReplyTokenEntity token) {
        mapper.updateById(token);
    }

    public boolean markUsedIfUnused(Long id, java.time.OffsetDateTime usedAt) {
        int rows = mapper.update(
                new QuestionReplyTokenEntity().setUsedAt(usedAt),
                new LambdaUpdateWrapper<QuestionReplyTokenEntity>()
                        .eq(QuestionReplyTokenEntity::getId, id)
                        .isNull(QuestionReplyTokenEntity::getUsedAt));
        return rows == 1;
    }
}
