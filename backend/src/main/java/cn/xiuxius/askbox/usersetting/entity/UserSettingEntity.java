package cn.xiuxius.askbox.usersetting.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("user_setting")
@Data
@Accessors(chain = true)
public class UserSettingEntity implements Serializable {
    @TableId("user_id")
    private Long userId;

    @TableField("key")
    private String key;

    private String value;
    private OffsetDateTime updatedAt;
}
