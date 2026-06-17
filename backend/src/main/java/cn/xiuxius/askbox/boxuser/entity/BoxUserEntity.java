package cn.xiuxius.askbox.boxuser.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("box_user")
@Data
@Accessors(chain = true)
public class BoxUserEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String slug;
    private String displayName;
    private String description;
    private Long avatarAttachmentId;
    private Long backgroundAttachmentId;

    @TableField("email_notify_enabled")
    private Boolean emailNotifyEnabled;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
