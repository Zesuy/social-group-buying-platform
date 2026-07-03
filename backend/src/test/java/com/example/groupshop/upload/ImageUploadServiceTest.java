package com.example.groupshop.upload;

import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.upload.config.UploadProperties;
import com.example.groupshop.upload.dto.ImageUploadResponse;
import com.example.groupshop.upload.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadServiceTest {

    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00
    };

    @TempDir
    Path tempDir;

    @Test
    void uploadImage_shouldSaveFileAndNormalizeOriginalFilename() throws Exception {
        ImageUploadService service = newService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\avatar.png",
                "image/png",
                PNG_BYTES
        );

        ImageUploadResponse response = service.uploadImage(file);

        assertThat(response.getUrl()).startsWith("http://localhost:8080/uploads/images/");
        assertThat(response.getObjectKey()).startsWith("images/");
        assertThat(response.getObjectKey()).endsWith(".png");
        assertThat(response.getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getSize()).isEqualTo(PNG_BYTES.length);
        assertThat(Files.exists(tempDir.resolve(response.getObjectKey()))).isTrue();
    }

    @Test
    void uploadImage_shouldRejectEmptyFile() {
        ImageUploadService service = newService(tempDir);
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上传图片不能为空");
    }

    @Test
    void uploadImage_shouldRejectUnsupportedContentType() {
        ImageUploadService service = newService(tempDir);
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void uploadImage_shouldRejectContentTypeMismatch() {
        ImageUploadService service = newService(tempDir);
        MockMultipartFile file = new MockMultipartFile("file", "bad.png", "image/png", "not-a-png".getBytes());

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容与类型不匹配");
    }

    @Test
    void uploadImage_shouldRejectOversizedFile() {
        ImageUploadService service = newService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[(int) ImageUploadService.MAX_IMAGE_SIZE_BYTES + 1]
        );

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片大小不能超过 5MB");
    }

    @Test
    void uploadImage_shouldFailWhenLocalDirIsAFile() throws Exception {
        Path fileAsDir = tempDir.resolve("not-a-dir");
        Files.writeString(fileAsDir, "occupied");
        ImageUploadService service = newService(fileAsDir);
        MockMultipartFile file = new MockMultipartFile("file", "ok.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片保存失败");
    }

    private ImageUploadService newService(Path localDir) {
        UploadProperties properties = new UploadProperties();
        properties.setLocalDir(localDir);
        properties.setPublicBaseUrl("http://localhost:8080/uploads");
        return new ImageUploadService(properties);
    }
}
