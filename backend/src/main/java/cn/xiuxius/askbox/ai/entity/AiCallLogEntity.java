package cn.xiuxius.askbox.ai.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.ai.enums.AiReviewTriggerType;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("ai_call_logs")
@Data
@Accessors(chain = true)
public class AiCallLogEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long questionId;
    private Long aiReviewId;
    private Long boxUserId;
    private AiReviewTriggerType triggerType;
    private Long triggeredBy;
    private String stage;
    private Boolean success;
    private String model;
    private String baseUrl;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
