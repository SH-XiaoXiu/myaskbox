package cn.xiuxius.askbox.attachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.service.ObjectStorageService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private AttachmentController controller;

    @Test
    void assetUsesAttachmentMetadataForStorageResponse() {
        AttachmentView attachment = new AttachmentView(
                1L,
                "avatar",
                AttachmentUsageType.ACCOUNT_AVATAR,
                AttachmentStorageType.S3,
                "uploads/account_avatar/1/a.png",
                "image/png",
                321L,
                "etag-321",
                null,
                0,
                true);
        ResponseEntity<?> expected = ResponseEntity.status(HttpStatus.FOUND).build();
        when(attachmentService.getByObjectKey("uploads/account_avatar/1/a.png")).thenReturn(attachment);
        doReturn(expected)
                .when(objectStorageService)
                .assetResponse("uploads/account_avatar/1/a.png", "image/png", 321L, "etag-321");

        ResponseEntity<?> actual = controller.asset("/uploads/account_avatar/1/a.png");

        assertThat(actual).isSameAs(expected);
        verify(objectStorageService).assetResponse("uploads/account_avatar/1/a.png", "image/png", 321L, "etag-321");
    }

    @Test
    void assetMapsMissingAttachmentToResourceNotFound() {
        when(attachmentService.getByObjectKey("missing.png"))
                .thenThrow(new BizException(ErrorCodes.ATTACHMENT_NOT_FOUND));

        BizException ex = assertThrows(BizException.class, () -> controller.asset("missing.png"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("图片不存在");
    }
}
