package com.example.groupshop.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.model.entity.UploadAsset;
import com.example.groupshop.model.entity.UploadAssetReference;
import com.example.groupshop.model.mapper.UploadAssetMapper;
import com.example.groupshop.model.mapper.UploadAssetReferenceMapper;
import com.example.groupshop.upload.config.UploadProperties;
import com.example.groupshop.upload.service.UploadAssetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UploadAssetServiceTest extends ServiceTestBase {

    private static final String OBJECT_PREFIX = "images/batch08-";

    @Autowired
    private UploadAssetService uploadAssetService;

    @Autowired
    private UploadAssetMapper uploadAssetMapper;

    @Autowired
    private UploadAssetReferenceMapper referenceMapper;

    @Autowired
    private UploadProperties uploadProperties;

    private long originalMaxTotalSizeMb;
    private long originalTemporaryRetentionHours;
    private int originalCleanupBatchSize;

    @BeforeEach
    void setUp() {
        originalMaxTotalSizeMb = uploadProperties.getMaxTotalSizeMb();
        originalTemporaryRetentionHours = uploadProperties.getTemporaryRetentionHours();
        originalCleanupBatchSize = uploadProperties.getCleanupBatchSize();
        cleanupRows();
        cleanupFiles();
    }

    @AfterEach
    void tearDown() {
        uploadProperties.setMaxTotalSizeMb(originalMaxTotalSizeMb);
        uploadProperties.setTemporaryRetentionHours(originalTemporaryRetentionHours);
        uploadProperties.setCleanupBatchSize(originalCleanupBatchSize);
        cleanupRows();
        cleanupFiles();
    }

    @Test
    void registerReferences_shouldDeduplicateAndMarkAssetInUse() {
        UploadAsset asset = createAsset("dedupe.png", 128);

        uploadAssetService.registerReferences("product", 501L, "coverImageUrl", List.of(asset.getUrl(), asset.getUrl()));

        UploadAsset updated = uploadAssetMapper.selectById(asset.getId());
        assertThat(updated.getStatus()).isEqualTo("in_use");
        assertThat(updated.getReferenceCount()).isEqualTo(1);
        assertThat(referenceMapper.selectCount(new LambdaQueryWrapper<UploadAssetReference>()
                .eq(UploadAssetReference::getAssetId, asset.getId()))).isEqualTo(1);
    }

    @Test
    void replaceReferences_shouldReleaseOldAssetAndUseNewAsset() {
        UploadAsset oldAsset = createAsset("old.png", 128);
        UploadAsset newAsset = createAsset("new.png", 128);
        uploadAssetService.registerReferences("store", 601L, "logoUrl", List.of(oldAsset.getUrl()));

        uploadAssetService.replaceReferences("store", 601L, "logoUrl", List.of(newAsset.getUrl()));

        UploadAsset released = uploadAssetMapper.selectById(oldAsset.getId());
        UploadAsset current = uploadAssetMapper.selectById(newAsset.getId());
        assertThat(released.getStatus()).isEqualTo("temporary");
        assertThat(released.getReferenceCount()).isZero();
        assertThat(current.getStatus()).isEqualTo("in_use");
        assertThat(current.getReferenceCount()).isEqualTo(1);
    }

    @Test
    void cleanupIfNeeded_shouldDeleteOnlyUnreferencedExpiredAssets() throws Exception {
        uploadProperties.setMaxTotalSizeMb(1);
        uploadProperties.setTemporaryRetentionHours(1);
        uploadProperties.setCleanupBatchSize(10);
        UploadAsset stale = createAsset("stale.png", 2 * 1024 * 1024L);
        UploadAsset referenced = createAsset("referenced.png", 2 * 1024 * 1024L);
        touchUploadFile(stale.getObjectKey());
        touchUploadFile(referenced.getObjectKey());
        markCreatedAt(stale.getId(), LocalDateTime.now().minusDays(2));
        markCreatedAt(referenced.getId(), LocalDateTime.now().minusDays(2));
        uploadAssetService.registerReferences("group_buy", 701L, "coverImageUrl", List.of(referenced.getUrl()));

        int deleted = uploadAssetService.cleanupIfNeeded();

        assertThat(deleted).isEqualTo(1);
        assertThat(uploadAssetMapper.selectById(stale.getId()).getStatus()).isEqualTo("deleted");
        assertThat(uploadAssetMapper.selectById(referenced.getId()).getStatus()).isEqualTo("in_use");
        assertThat(Files.exists(uploadProperties.getLocalDir().resolve(stale.getObjectKey()))).isFalse();
        assertThat(Files.exists(uploadProperties.getLocalDir().resolve(referenced.getObjectKey()))).isTrue();
    }

    private UploadAsset createAsset(String filename, long sizeBytes) {
        String objectKey = OBJECT_PREFIX + filename;
        String url = uploadProperties.getPublicBaseUrl() + "/" + objectKey;
        return uploadAssetService.recordUpload(
                801L,
                objectKey,
                url,
                filename,
                "image/png",
                sizeBytes,
                new byte[] {1, 2, 3, 4}
        );
    }

    private void markCreatedAt(Long assetId, LocalDateTime createdAt) {
        uploadAssetMapper.update(null, new LambdaUpdateWrapper<UploadAsset>()
                .eq(UploadAsset::getId, assetId)
                .set(UploadAsset::getCreatedAt, createdAt));
    }

    private void touchUploadFile(String objectKey) throws Exception {
        Path path = uploadProperties.getLocalDir().resolve(objectKey);
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[] {1, 2, 3});
    }

    private void cleanupRows() {
        referenceMapper.delete(new LambdaQueryWrapper<UploadAssetReference>()
                .likeRight(UploadAssetReference::getObjectKey, OBJECT_PREFIX));
        uploadAssetMapper.delete(new LambdaQueryWrapper<UploadAsset>()
                .likeRight(UploadAsset::getObjectKey, OBJECT_PREFIX));
    }

    private void cleanupFiles() {
        Path dir = uploadProperties.getLocalDir().resolve("images");
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.getFileName().toString().startsWith("batch08-"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // Best-effort cleanup for test artifacts.
                        }
                    });
        } catch (Exception ignored) {
            // Best-effort cleanup for test artifacts.
        }
    }
}
