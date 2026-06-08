package cn.xiuxius.askbox.attachment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.common.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "附件接口", description = "公开附件接口")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/api/attachments/anonymous-avatars")
    @Operation(summary = "获取匿名头像附件列表", description = "返回所有启用的匿名头像附件。")
    public R<List<AttachmentView>> anonymousAvatars() {
        return R.ok(attachmentService.listAnonymousAvatars());
    }
}
