package cn.xiuxius.askbox.attachment.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("attachments")
@Data
@Accessors(chain = true)
public class AttachmentEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;
    private AttachmentUsageType usageType;
    private AttachmentStorageType storageType;
    private String objectKey;
    private String mimeType;
    private Long sizeBytes;
    private String sha256;
    private String bg;
    private Integer sortOrder;
    private Boolean isActive;
    private String ownerType;
    private Long ownerId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
