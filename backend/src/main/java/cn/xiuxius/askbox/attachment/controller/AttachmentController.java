package cn.xiuxius.askbox.attachment.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.attachment.request.UploadPresignRequest;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.service.ObjectStorageService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.attachment.view.UploadPresignView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "附件接口", description = "公开附件接口")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final ObjectStorageService objectStorageService;

    @GetMapping("/api/attachments/anonymous-avatars")
    @Operation(summary = "获取匿名头像附件列表", description = "返回所有启用的匿名头像附件。")
    public R<List<AttachmentView>> anonymousAvatars() {
        return R.ok(attachmentService.listAnonymousAvatars());
    }

    @PostMapping("/api/attachments/uploads/presign")
    @Operation(summary = "生成附件直传签名", description = "登录用户获取 MinIO 前端直传表单。")
    public R<UploadPresignView> presignUpload(@Valid @RequestBody UploadPresignRequest request) {
        StpUtil.checkLogin();
        return R.ok(attachmentService.presignUpload(
                request.getUsageType(), request.getFileName(), request.getMimeType(), request.getSizeBytes()));
    }

    @GetMapping("/api/assets/{*objectKey}")
    @Operation(summary = "读取附件图片", description = "根据对象 key 返回图片重定向或代理流。")
    public ResponseEntity<?> asset(@PathVariable String objectKey) {
        String normalized = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        if (!attachmentService.existsByObjectKey(normalized)) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "图片不存在");
        }
        return objectStorageService.assetResponse(normalized);
    }
}
