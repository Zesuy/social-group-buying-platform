package com.example.groupshop.upload.service;

import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.upload.config.UploadProperties;
import com.example.groupshop.upload.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final UploadProperties uploadProperties;

    public ImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上传图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片大小不能超过 5MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅支持 jpg、png、webp 图片");
        }

        byte[] bytes = readBytes(file);
        validateMagic(bytes, contentType);

        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String objectKey = "images/" + datePath + "/" + UUID.randomUUID() + "." + extension;
        Path target = uploadProperties.getLocalDir().resolve(objectKey).normalize();
        Path root = uploadProperties.getLocalDir().toAbsolutePath().normalize();
        Path absoluteTarget = target.toAbsolutePath().normalize();
        if (!absoluteTarget.startsWith(root)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传路径不合法");
        }

        try {
            Files.createDirectories(absoluteTarget.getParent());
            Files.write(absoluteTarget, bytes);
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }

        return ImageUploadResponse.builder()
                .url(buildPublicUrl(objectKey))
                .objectKey(objectKey)
                .originalFilename(normalizeOriginalFilename(file.getOriginalFilename()))
                .contentType(contentType)
                .size(file.getSize())
                .build();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传图片失败");
        }
    }

    private static String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private static String normalizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        return originalFilename.replace("\\", "/").substring(originalFilename.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private void validateMagic(byte[] bytes, String contentType) {
        boolean valid = switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4e
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0d
                    && bytes[5] == 0x0a
                    && bytes[6] == 0x1a
                    && bytes[7] == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 0x52
                    && bytes[1] == 0x49
                    && bytes[2] == 0x46
                    && bytes[3] == 0x46
                    && bytes[8] == 0x57
                    && bytes[9] == 0x45
                    && bytes[10] == 0x42
                    && bytes[11] == 0x50;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "图片文件内容与类型不匹配");
        }
    }

    private String buildPublicUrl(String objectKey) {
        String baseUrl = uploadProperties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + objectKey;
        }
        return baseUrl + "/" + objectKey;
    }
}
