package cn.xiuxius.askbox.box.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.boxuser.view.PublicBoxProfileView;
import cn.xiuxius.askbox.common.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 公开提问箱资料接口。 */
@RestController
@RequestMapping("/api/boxes")
@RequiredArgsConstructor
@Tag(name = "公开接口", description = "匿名提问箱资料（无需登录）")
public class PublicBoxController {

    private final BoxUserService boxUserService;

    @GetMapping("/{slug}")
    @Operation(summary = "获取公开提问箱资料", description = "返回公开页顶部展示所需的箱子名称和简介。")
    public R<PublicBoxProfileView> profile(@Parameter(description = "提问箱 slug") @PathVariable String slug) {
        return R.ok(boxUserService.getPublicProfileBySlug(slug));
    }
}
