package cn.xiuxius.askbox.system.setting.view;

public record SysSettingView(
        String key,
        String value,
        String valueType,
        String groupCode,
        String label,
        String description,
        Boolean isSecret) {}
