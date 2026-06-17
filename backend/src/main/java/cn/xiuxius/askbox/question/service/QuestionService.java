package cn.xiuxius.askbox.question.service;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.view.AdminQuestionView;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.question.view.QuestionView;

public interface QuestionService {
    void submit(String slug, Long attachmentId, String question, String ip, String userAgent, String origin);

    PageResult<QuestionView> getPublished(String slug, long page, long pageSize);

    PageResult<PendingQuestionView> getPending(Long boxUserId, long page, long pageSize);

    /** 获取箱主的历史问题（已发布/已驳回），按 status 过滤。返回 QuestionView（含回答）。 */
    PageResult<QuestionView> getHistory(Long boxUserId, String status, long page, long pageSize);

    QuestionView answer(Long boxUserId, Long questionId, String answerContent, Long answeredBy, String ip, String ua);

    void dismiss(Long boxUserId, Long questionId);

    void delete(Long boxUserId, Long questionId);

    /** 根据 ID 查询单个问题视图。 */
    QuestionView getById(Long questionId);

    /** 根据 ID 查询管理端问题视图（含 boxSlug + status）。 */
    AdminQuestionView getAdminById(Long questionId);

    /** 管理端问题列表（含 boxSlug + status）。 */
    PageResult<AdminQuestionView> listAllAdmin(long page, long pageSize, Long boxUserId, String status, String keyword);

    /** 保留旧方法：管理端基础问题列表。 */
    PageResult<QuestionView> listAll(long page, long pageSize, Long boxUserId, String status, String keyword);

    void forceDelete(Long questionId);
}
