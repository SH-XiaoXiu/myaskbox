package cn.xiuxius.askbox.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.request.AttachmentSaveRequest;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/attachments")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminAttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    @Operation(summary = "附件列表（管理）")
    public R<PageResult<AttachmentView>> listAttachments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) AttachmentUsageType usageType) {
        return R.ok(attachmentService.pageAll(page, pageSize, usageType));
    }

    @PostMapping
    @Operation(summary = "新增附件")
    public R<AttachmentView> createAttachment(@Valid @RequestBody AttachmentSaveRequest request) {
        return R.ok(attachmentService.create(
                request.getName(),
                request.getUsageType(),
                request.getContentBase64(),
                request.getBg(),
                request.getSortOrder()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑附件")
    public R<AttachmentView> updateAttachment(
            @PathVariable Long id, @Valid @RequestBody AttachmentSaveRequest request) {
        return R.ok(attachmentService.update(
                id,
                request.getName(),
                request.getUsageType(),
                request.getContentBase64(),
                request.getBg(),
                request.getSortOrder(),
                request.getIsActive()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除附件")
    public R<Void> deleteAttachment(@PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }
}
