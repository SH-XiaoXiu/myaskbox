package cn.xiuxius.askbox.answer.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.repository.AnswerRepository;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.service.LikeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerServiceImpl implements AnswerService {
    private final AnswerRepository answerRepository;
    private final LikeService likeService;

    @Override
    @Transactional
    public AnswerEntity create(Long questionId, String content, Long answeredBy, String ip, String userAgent) {
        AnswerEntity a = new AnswerEntity()
                .setQuestionId(questionId)
                .setContent(content)
                .setAnsweredBy(answeredBy)
                .setIp(ip)
                .setUserAgent(userAgent)
                .setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        answerRepository.insert(a);
        log.info("AnswerEntity created: id={} questionId={} by={}", a.getId(), questionId, answeredBy);
        return a;
    }

    @Override
    public AnswerEntity getByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    @Override
    public AnswerEntity getById(Long id) {
        AnswerEntity a = answerRepository.findById(id);
        if (a == null) throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "回答不存在");
        return a;
    }

    @Override
    public void delete(Long id) {
        answerRepository.deleteById(id);
        likeService.deleteTarget(LikeTargetType.ANSWER, id);
    }

    @Override
    public void deleteByQuestionIdIfExists(Long questionId) {
        AnswerEntity answer = answerRepository.findByQuestionId(questionId);
        answerRepository.deleteByQuestionId(questionId);
        if (answer != null) {
            likeService.deleteTarget(LikeTargetType.ANSWER, answer.getId());
        }
    }
}
