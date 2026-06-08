package cn.xiuxius.askbox.system.setting.assembler;

import cn.xiuxius.askbox.system.setting.entity.SysSettingEntity;
import cn.xiuxius.askbox.system.setting.view.SysSettingView;

public final class SysSettingAssembler {

    private SysSettingAssembler() {}

    public static SysSettingView toView(SysSettingEntity entity) {
        String value =
                Boolean.TRUE.equals(entity.getIsSecret()) && hasText(entity.getValue()) ? "******" : entity.getValue();
        return new SysSettingView(
                entity.getKey(),
                value,
                entity.getValueType(),
                entity.getGroupCode(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getIsSecret());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
