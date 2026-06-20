package cn.xiuxius.askbox.topic.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.topic.enums.TopicStatus;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("box_topics")
@Data
@Accessors(chain = true)
public class BoxTopicEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("box_user_id")
    private Long boxUserId;

    private String code;
    private String title;
    private String description;
    private TopicStatus status;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    @TableField("closed_at")
    private OffsetDateTime closedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    public boolean isOwnedByBox(Long boxUserId) {
        return this.boxUserId != null && this.boxUserId.equals(boxUserId);
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt == null || !expiresAt.isAfter(now);
    }

    public boolean isAvailable(OffsetDateTime now) {
        return status == TopicStatus.ACTIVE && !isExpired(now);
    }
}
