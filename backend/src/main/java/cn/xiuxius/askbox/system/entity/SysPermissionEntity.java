package cn.xiuxius.askbox.system.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("sys_permission")
@Data
public class SysPermissionEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private Long parentId;
    private String type;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
