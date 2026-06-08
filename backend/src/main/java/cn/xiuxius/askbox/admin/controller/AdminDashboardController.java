package cn.xiuxius.askbox.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.attachment.repository.AttachmentRepository;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.system.repository.SysUserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminDashboardController {

    private final SysUserRepository sysUserRepository;
    private final BoxUserRepository boxUserRepository;
    private final QuestionRepository questionRepository;
    private final AttachmentRepository attachmentRepository;

    @GetMapping("/stats")
    @Operation(summary = "仪表盘统计", description = "返回用户数、提问箱数、问题数、头像数。")
    public R<DashboardStatsView> dashboardStats() {
        return R.ok(new DashboardStatsView(
                sysUserRepository.countAll(),
                boxUserRepository.countAll(),
                questionRepository.countAll(),
                attachmentRepository.countAll()));
    }
}
