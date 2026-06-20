package cn.xiuxius.askbox.like.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.xiuxius.askbox.like.enums.LikeTargetType;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("like_counts")
@Data
@Accessors(chain = true)
public class LikeCountEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("target_type")
    private LikeTargetType targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("like_count")
    private Long likeCount;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
