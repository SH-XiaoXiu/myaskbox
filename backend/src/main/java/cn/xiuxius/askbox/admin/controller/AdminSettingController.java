package cn.xiuxius.askbox.admin.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.system.setting.request.SysSettingUpdateRequest;
import cn.xiuxius.askbox.system.setting.service.SysSettingService;
import cn.xiuxius.askbox.system.setting.view.SysSettingView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统设置接口", description = "系统管理员配置注册与邮件")
public class AdminSettingController {

    private final SysSettingService sysSettingService;

    @GetMapping
    @Operation(summary = "系统设置列表")
    public R<java.util.List<SysSettingView>> list() {
        return R.ok(sysSettingService.list());
    }

    @PutMapping
    @Operation(summary = "批量更新系统设置")
    public R<Void> update(@Valid @RequestBody SysSettingUpdateRequest request) {
        Map<String, String> values = new HashMap<>();
        for (SysSettingUpdateRequest.Item item : request.getItems()) {
            values.put(item.getKey(), item.getValue());
        }
        sysSettingService.update(values);
        return R.ok();
    }
}
