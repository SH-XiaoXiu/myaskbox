package cn.xiuxius.askbox.system.setting.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SysSettingUpdateRequest {
    @Valid
    @NotNull(message = "设置项不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "设置 key 不能为空")
        private String key;

        private String value;
    }
}
