package cn.xiuxius.askbox.question.assembler;

import java.time.OffsetDateTime;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.avatar.assembler.AvatarAssembler;
import cn.xiuxius.askbox.avatar.entity.AvatarEntity;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.view.AdminQuestionView;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.question.view.QuestionView;

public final class QuestionAssembler {

    private QuestionAssembler() {}

    public static PendingQuestionView toPendingView(QuestionEntity question, AvatarEntity avatar) {
        return new PendingQuestionView(
                question.getId(),
                AvatarAssembler.toView(avatar),
                question.getQuestion(),
                toEpochMillis(question.getCreatedAt(), question.getUpdatedAt()));
    }

    public static QuestionView toQuestionView(QuestionEntity question, AvatarEntity avatar, AnswerEntity answer) {
        OffsetDateTime fallback = question.getUpdatedAt() != null ? question.getUpdatedAt() : question.getCreatedAt();
        long ts = answer != null
                ? toEpochMillis(answer.getCreatedAt(), fallback)
                : toEpochMillis(question.getCreatedAt(), fallback);
        return new QuestionView(
                question.getId(),
                AvatarAssembler.toView(avatar),
                question.getQuestion(),
                answer != null ? answer.getContent() : null,
                ts);
    }

    public static AdminQuestionView toAdminView(QuestionEntity question, String boxSlug, AnswerEntity answer) {
        return new AdminQuestionView(
                question.getId(),
                boxSlug,
                question.getQuestion(),
                question.getStatus().name(),
                answer != null ? answer.getContent() : null,
                toEpochMillis(question.getCreatedAt(), question.getUpdatedAt()),
                answer != null ? toEpochMillis(answer.getCreatedAt(), question.getUpdatedAt()) : null);
    }

    private static long toEpochMillis(OffsetDateTime value, OffsetDateTime fallback) {
        OffsetDateTime ts = value != null ? value : fallback;
        return ts != null ? ts.toInstant().toEpochMilli() : 0L;
    }
}
