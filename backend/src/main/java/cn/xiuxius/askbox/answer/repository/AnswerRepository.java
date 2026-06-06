package cn.xiuxius.askbox.answer.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.mapper.AnswerMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnswerRepository {
    private final AnswerMapper mapper;

    public void insert(AnswerEntity a) {
        mapper.insert(a);
    }

    public AnswerEntity findByQuestionId(Long questionId) {
        return mapper.selectOne(new LambdaQueryWrapper<AnswerEntity>().eq(AnswerEntity::getQuestionId, questionId));
    }

    public AnswerEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public void deleteByQuestionId(Long questionId) {
        mapper.delete(new LambdaQueryWrapper<AnswerEntity>().eq(AnswerEntity::getQuestionId, questionId));
    }
}
