package cn.xiuxius.askbox.system.request;

import java.util.List;

import lombok.Data;

@Data
public class UserRolesUpdateRequest {
    private List<String> roleCodes;
}
