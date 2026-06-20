package cn.xiuxius.askbox.system.setting.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

@TableName("sys_setting")
@Data
@Accessors(chain = true)
public class SysSettingEntity implements Serializable {
    @TableId
    private String key;

    private String value;
    private String valueType;
    private String groupCode;
    private String label;
    private String description;
    private Boolean isSecret;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
