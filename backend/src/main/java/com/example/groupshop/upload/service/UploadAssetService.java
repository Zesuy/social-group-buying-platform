package com.example.groupshop.upload.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.groupshop.model.entity.UploadAsset;
import com.example.groupshop.model.entity.UploadAssetReference;
import com.example.groupshop.model.mapper.UploadAssetMapper;
import com.example.groupshop.model.mapper.UploadAssetReferenceMapper;
import com.example.groupshop.upload.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadAssetService {

    private final UploadAssetMapper uploadAssetMapper;
    private final UploadAssetReferenceMapper referenceMapper;
    private final UploadProperties uploadProperties;

    @Transactional
    public UploadAsset recordUpload(Long uploaderUserId, String objectKey, String url, String originalFilename,
                                    String contentType, long sizeBytes, byte[] bytes) {
        UploadAsset asset = new UploadAsset();
        asset.setUploaderUserId(uploaderUserId);
        asset.setObjectKey(objectKey);
        asset.setUrl(url);
        asset.setOriginalFilename(originalFilename);
        asset.setContentType(contentType);
        asset.setSizeBytes(sizeBytes);
        asset.setChecksumSha256(sha256(bytes));
        asset.setStatus("temporary");
        asset.setReferenceCount(0);
        uploadAssetMapper.insert(asset);
        return asset;
    }

    @Transactional
    public void replaceReferences(String refType, Long refId, String fieldName, Collection<String> urls) {
        releaseReferences(refType, refId, fieldName);
        registerReferences(refType, refId, fieldName, urls);
    }

    @Transactional
    public void registerReferences(String refType, Long refId, String fieldName, Collection<String> urls) {
        if (refId == null || urls == null || urls.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (String url : normalizeUrls(urls)) {
            UploadAsset asset = findByUrl(url);
            if (asset == null || "deleted".equals(asset.getStatus())) {
                continue;
            }
            Long existing = referenceMapper.selectCount(new LambdaQueryWrapper<UploadAssetReference>()
                    .eq(UploadAssetReference::getAssetId, asset.getId())
                    .eq(UploadAssetReference::getRefType, refType)
                    .eq(UploadAssetReference::getRefId, refId)
                    .eq(UploadAssetReference::getFieldName, fieldName));
            if (existing != null && existing > 0) {
                continue;
            }

            UploadAssetReference reference = new UploadAssetReference();
            reference.setAssetId(asset.getId());
            reference.setObjectKey(asset.getObjectKey());
            reference.setRefType(refType);
            reference.setRefId(refId);
            reference.setFieldName(fieldName);
            referenceMapper.insert(reference);

            uploadAssetMapper.update(null, new LambdaUpdateWrapper<UploadAsset>()
                    .eq(UploadAsset::getId, asset.getId())
                    .setSql("reference_count = reference_count + 1")
                    .set(UploadAsset::getStatus, "in_use")
                    .set(UploadAsset::getLastReferencedAt, now));
        }
    }

    @Transactional
    public void releaseReferences(String refType, Long refId, String fieldName) {
        if (refId == null) {
            return;
        }
        List<UploadAssetReference> references = referenceMapper.selectList(new LambdaQueryWrapper<UploadAssetReference>()
                .eq(UploadAssetReference::getRefType, refType)
                .eq(UploadAssetReference::getRefId, refId)
                .eq(UploadAssetReference::getFieldName, fieldName));
        if (references.isEmpty()) {
            return;
        }
        for (UploadAssetReference reference : references) {
            referenceMapper.deleteById(reference.getId());
            long remaining = referenceMapper.selectCount(new LambdaQueryWrapper<UploadAssetReference>()
                    .eq(UploadAssetReference::getAssetId, reference.getAssetId()));
            uploadAssetMapper.update(null, new LambdaUpdateWrapper<UploadAsset>()
                    .eq(UploadAsset::getId, reference.getAssetId())
                    .set(UploadAsset::getReferenceCount, (int) remaining)
                    .set(UploadAsset::getStatus, remaining > 0 ? "in_use" : "temporary"));
        }
    }

    public int cleanupIfNeeded() {
        long maxBytes = uploadProperties.getMaxTotalSizeMb() * 1024L * 1024L;
        long currentBytes = uploadAssetMapper.selectList(new LambdaQueryWrapper<UploadAsset>()
                        .ne(UploadAsset::getStatus, "deleted"))
                .stream()
                .mapToLong(asset -> asset.getSizeBytes() == null ? 0L : asset.getSizeBytes())
                .sum();
        if (currentBytes <= maxBytes) {
            return 0;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(uploadProperties.getTemporaryRetentionHours());
        List<UploadAsset> candidates = uploadAssetMapper.selectList(new LambdaQueryWrapper<UploadAsset>()
                .ne(UploadAsset::getStatus, "deleted")
                .eq(UploadAsset::getReferenceCount, 0)
                .lt(UploadAsset::getCreatedAt, cutoff)
                .orderByAsc(UploadAsset::getCreatedAt)
                .last("LIMIT " + Math.max(1, uploadProperties.getCleanupBatchSize())));

        int deleted = 0;
        for (UploadAsset asset : candidates) {
            long refs = referenceMapper.selectCount(new LambdaQueryWrapper<UploadAssetReference>()
                    .eq(UploadAssetReference::getAssetId, asset.getId()));
            if (refs > 0) {
                continue;
            }
            Path path = resolveAssetPath(asset.getObjectKey());
            if (path == null) {
                continue;
            }
            try {
                Files.deleteIfExists(path);
                markDeleted(asset.getId());
                deleted++;
            } catch (IOException | RuntimeException ex) {
                log.warn("Failed to cleanup upload asset {}", asset.getObjectKey(), ex);
            }
        }
        return deleted;
    }

    public UploadAsset findByUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String objectKey = extractObjectKey(url);
        if (objectKey == null) {
            return null;
        }
        return uploadAssetMapper.selectOne(new LambdaQueryWrapper<UploadAsset>()
                .eq(UploadAsset::getObjectKey, objectKey));
    }

    public String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String base = uploadProperties.getPublicBaseUrl();
        String normalizedBase = base.endsWith("/") ? base : base + "/";
        if (!url.startsWith(normalizedBase)) {
            return null;
        }
        String objectKey = url.substring(normalizedBase.length());
        if (objectKey.isBlank() || objectKey.contains("..") || objectKey.startsWith("/")) {
            return null;
        }
        return objectKey;
    }

    private Set<String> normalizeUrls(Collection<String> urls) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String url : urls) {
            if (url != null && !url.isBlank()) {
                normalized.add(url.trim());
            }
        }
        return normalized;
    }

    private Path resolveAssetPath(String objectKey) {
        Path root = uploadProperties.getLocalDir().toAbsolutePath().normalize();
        Path path = root.resolve(objectKey).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            log.warn("Reject cleanup path outside upload root: {}", objectKey);
            return null;
        }
        return path;
    }

    private void markDeleted(Long assetId) {
        uploadAssetMapper.update(null, new LambdaUpdateWrapper<UploadAsset>()
                .eq(UploadAsset::getId, assetId)
                .set(UploadAsset::getStatus, "deleted")
                .set(UploadAsset::getDeletedAt, LocalDateTime.now())
                .set(UploadAsset::getReferenceCount, 0));
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
