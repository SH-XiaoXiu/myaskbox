package cn.xiuxius.askbox.system.setting.service;

import java.util.List;
import java.util.Map;

import cn.xiuxius.askbox.system.setting.view.SysSettingView;

public interface SysSettingService {

    List<SysSettingView> list();

    void update(Map<String, String> values);

    String getString(String key, String defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    int getInt(String key, int defaultValue);
}
