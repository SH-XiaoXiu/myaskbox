package cn.xiuxius.askbox.system.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("sys_user")
@Data
@Accessors(chain = true)
public class SysUserEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String passwordHash;
    private String displayName;
    private String email;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long avatarAttachmentId;

    private String status;
    private Integer topicActiveLimit;
    private OffsetDateTime lastLoginAt;
    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
