package cn.xiuxius.askbox.question.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.entity.QuestionReplyTokenEntity;
import cn.xiuxius.askbox.question.repository.QuestionReplyTokenRepository;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.question.view.QuestionView;
import cn.xiuxius.askbox.question.view.ReplyTokenQuestionView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReplyTokenServiceImpl implements ReplyTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final QuestionReplyTokenRepository tokenRepository;
    private final QuestionRepository questionRepository;
    private final BoxUserRepository boxUserRepository;
    private final QuestionService questionService;

    public String create(Long questionId, Long boxUserId, OffsetDateTime expiresAt) {
        String rawToken = newToken();
        QuestionReplyTokenEntity entity = new QuestionReplyTokenEntity()
                .setQuestionId(questionId)
                .setBoxUserId(boxUserId)
                .setTokenHash(hash(rawToken))
                .setExpiresAt(expiresAt)
                .setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        tokenRepository.insert(entity);
        return rawToken;
    }

    @Override
    public ReplyTokenQuestionView get(String token) {
        QuestionReplyTokenEntity replyToken = validToken(token);
        QuestionEntity question = findQuestion(replyToken);
        BoxUserEntity box = boxUserRepository.findById(replyToken.getBoxUserId());
        String displayName = box == null ? "" : box.getDisplayName();
        return new ReplyTokenQuestionView(
                question.getId(),
                displayName,
                question.getQuestion(),
                replyToken.getExpiresAt().toInstant().toEpochMilli());
    }

    @Override
    @Transactional
    public QuestionView answer(String token, String answer, String ip, String userAgent) {
        QuestionReplyTokenEntity replyToken = validToken(token);
        QuestionEntity question = findQuestion(replyToken);
        if (!tokenRepository.markUsedIfUnused(replyToken.getId(), OffsetDateTime.now(ZoneOffset.UTC))) {
            throw invalidToken();
        }
        QuestionView view = questionService.answer(
                replyToken.getBoxUserId(), question.getId(), answer, boxOwnerUserId(replyToken), ip, userAgent);
        return view;
    }

    private Long boxOwnerUserId(QuestionReplyTokenEntity replyToken) {
        BoxUserEntity box = boxUserRepository.findById(replyToken.getBoxUserId());
        if (box == null) {
            throw new BizException(ErrorCodes.BOX_NOT_FOUND);
        }
        return box.getUserId();
    }

    private QuestionEntity findQuestion(QuestionReplyTokenEntity replyToken) {
        QuestionEntity question = questionRepository.findById(replyToken.getQuestionId());
        if (question == null || !question.isOwnedByBox(replyToken.getBoxUserId())) {
            throw invalidToken();
        }
        if (!question.isPending()) {
            throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "问题不是待回答状态");
        }
        return question;
    }

    private QuestionReplyTokenEntity validToken(String token) {
        QuestionReplyTokenEntity replyToken = tokenRepository.findByTokenHash(hash(token));
        if (replyToken == null
                || replyToken.getUsedAt() != null
                || !replyToken.getExpiresAt().isAfter(now())) {
            throw invalidToken();
        }
        return replyToken;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private BizException invalidToken() {
        return new BizException(ErrorCodes.VALIDATION_ERROR, "回复链接无效或已过期");
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Token hash failed", ex);
        }
    }
}
