package cn.xiuxius.askbox.question.service;

import cn.xiuxius.askbox.question.view.QuestionView;
import cn.xiuxius.askbox.question.view.ReplyTokenQuestionView;

public interface ReplyTokenService {
    ReplyTokenQuestionView get(String token);

    QuestionView answer(String token, String answer, String ip, String userAgent);
}
