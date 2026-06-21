package cn.xiuxius.askbox.ai.service;

import java.util.List;
import java.util.Map;

import cn.xiuxius.askbox.ai.view.AiReviewView;

public interface AiReviewService {
    void enqueueAuto(Long questionId);

    AiReviewView enqueueManual(Long questionId, Long adminUserId);

    AiReviewView getByQuestionId(Long questionId);

    AiReviewView getAdminByQuestionId(Long questionId);

    List<AiReviewView> batchSucceededPublic(Iterable<Long> questionIds);

    Map<Long, AiReviewView> succeededByQuestionIds(Iterable<Long> questionIds);

    void deleteByQuestionId(Long questionId);
}
