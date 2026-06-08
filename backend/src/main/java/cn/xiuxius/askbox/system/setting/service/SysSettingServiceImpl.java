package cn.xiuxius.askbox.system.setting.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.system.setting.assembler.SysSettingAssembler;
import cn.xiuxius.askbox.system.setting.entity.SysSettingEntity;
import cn.xiuxius.askbox.system.setting.repository.SysSettingRepository;
import cn.xiuxius.askbox.system.setting.view.SysSettingView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SysSettingServiceImpl implements SysSettingService {

    private final SysSettingRepository repository;

    @Override
    public List<SysSettingView> list() {
        return repository.findAll().stream().map(SysSettingAssembler::toView).toList();
    }

    @Override
    @Transactional
    public void update(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Map<String, SysSettingEntity> settings = repository.findByKeys(values.keySet()).stream()
                .collect(Collectors.toMap(SysSettingEntity::getKey, Function.identity()));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            SysSettingEntity setting = settings.get(entry.getKey());
            if (setting == null) throw new BizException(ErrorCodes.VALIDATION_ERROR, "设置项不存在: " + entry.getKey());
            String next = normalizedValue(setting, entry.getValue());
            if (Boolean.TRUE.equals(setting.getIsSecret())
                    && (next == null || next.isBlank() || "******".equals(next))) {
                continue;
            }
            validateValue(setting, next);
            setting.setValue(next).setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            repository.update(setting);
        }
    }

    private String normalizedValue(SysSettingEntity setting, String value) {
        if (value == null || Boolean.TRUE.equals(setting.getIsSecret())) {
            return value;
        }
        return value.trim();
    }

    @Override
    public String getString(String key, String defaultValue) {
        SysSettingEntity setting = repository.findByKey(key);
        String value = setting != null ? setting.getValue() : null;
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void validateValue(SysSettingEntity setting, String value) {
        if ("BOOLEAN".equals(setting.getValueType())
                && value != null
                && !value.isBlank()
                && !"true".equalsIgnoreCase(value)
                && !"false".equalsIgnoreCase(value)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, setting.getKey() + " 必须是 true 或 false");
        }
        if ("NUMBER".equals(setting.getValueType()) && value != null && !value.isBlank()) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new BizException(ErrorCodes.VALIDATION_ERROR, setting.getKey() + " 必须是数字");
            }
        }
    }
}
