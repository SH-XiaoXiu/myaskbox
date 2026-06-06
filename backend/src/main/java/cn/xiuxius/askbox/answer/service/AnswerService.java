package cn.xiuxius.askbox.answer.service;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;

public interface AnswerService {
    AnswerEntity create(Long questionId, String content, Long answeredBy, String ip, String userAgent);

    AnswerEntity getByQuestionId(Long questionId);

    AnswerEntity getById(Long id);

    void delete(Long id);

    void deleteByQuestionIdIfExists(Long questionId);
}
