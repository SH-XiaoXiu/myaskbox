package cn.xiuxius.askbox.system.view;

import java.time.OffsetDateTime;

public record PermissionView(
        Long id, String code, String name, Long parentId, String type, Integer sortOrder, OffsetDateTime createdAt) {}
