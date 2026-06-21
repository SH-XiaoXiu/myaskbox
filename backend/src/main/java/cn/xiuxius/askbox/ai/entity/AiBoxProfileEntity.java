package cn.xiuxius.askbox.ai.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("ai_box_profiles")
@Data
@Accessors(chain = true)
public class AiBoxProfileEntity implements Serializable {
    @TableId
    private Long boxUserId;

    private String styleSummary;
    private Integer sampleCount;
    private Integer version;
    private OffsetDateTime updatedAt;
}
