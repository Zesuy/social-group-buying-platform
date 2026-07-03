package com.example.groupshop.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.browsing.service.BrowsingHistoryService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.common.dto.ContentBlockRequest;
import com.example.groupshop.common.dto.ProductSummaryData;
import com.example.groupshop.common.util.ContentValidationUtil;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.common.util.DistanceCalculator;
import com.example.groupshop.favorite.service.FavoriteService;
import com.example.groupshop.groupbuy.dto.CreateDraftGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest.InlineProduct;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest.ItemEntry;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse.GroupBuyData;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse.GroupBuyItemData;
import com.example.groupshop.groupbuy.dto.ShareCardResponse;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyPermissionRequest;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest.UpdateItemEntry;
import com.example.groupshop.notification.service.NotificationService;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.entity.GroupBuyShareToken;
import com.example.groupshop.model.mapper.GroupBuyMapper;
import com.example.groupshop.model.mapper.GroupBuyShareTokenMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.OrderItemMapper;
import com.example.groupshop.model.mapper.ProductMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse.GroupBuyDetailItemData;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse.LeaderDetail;
import com.example.groupshop.publicbrowsing.dto.GroupBuyDetailResponse.StoreDetail;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem.LeaderLite;
import com.example.groupshop.publicbrowsing.dto.PublicGroupBuyItem.StoreLite;
import com.example.groupshop.publicbrowsing.dto.ViewerInfo;
import com.example.groupshop.subscription.service.SubscriptionService;
import com.example.groupshop.upload.service.UploadAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for creating and managing group buys under the current user's store.
 */
@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyMapper groupBuyMapper;
    private final GroupBuyItemMapper groupBuyItemMapper;
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final LeaderMapper leaderMapper;
    private final StoreMapper storeMapper;
    private final CurrentStoreHelper currentStoreHelper;
    private final SubscriptionService subscriptionService;
    private final FavoriteService favoriteService;
    private final BrowsingHistoryService browsingHistoryService;
    private final GroupBuyShareTokenMapper groupBuyShareTokenMapper;
    private final ContentValidationUtil contentValidationUtil;
    private final UploadAssetService uploadAssetService;
    private final NotificationService notificationService;

    // ── Create ────────────────────────────────────────────────────────

    @Transactional
    public GroupBuyResponse createGroupBuy(Long userId, CreateGroupBuyRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();
        Leader leader = ls.getLeader();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购至少包含一个商品");
        }

        LocalDateTime startTime = parseIsoDateTime(request.getStartTime());
        LocalDateTime endTime = parseIsoDateTime(request.getEndTime());
        LocalDateTime shippingTime = parseIsoDateTime(request.getShippingTime());

        // Determine groupType: support from request, default to "normal"
        String groupType = request.getGroupType();
        if (groupType == null || groupType.isBlank()) {
            groupType = "normal";
        } else if (!"normal".equals(groupType) && !"presale".equals(groupType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购类型只允许 normal 或 presale");
        }

        // Presale time validation
        if ("presale".equals(groupType)) {
            if (startTime == null || endTime == null || shippingTime == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "预售团购必须指定 startTime、endTime 和 shippingTime");
            }
            if (!endTime.isAfter(startTime)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束时间必须晚于开始时间");
            }
            if (!shippingTime.isAfter(endTime)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "发货时间必须晚于结束时间");
            }
        } else {
            validateEndTimeAfterStart(startTime, endTime);
        }

        GroupBuy groupBuy = new GroupBuy();
        groupBuy.setStoreId(store.getId());
        groupBuy.setLeaderId(leader.getId());
        groupBuy.setTitle(request.getTitle());
        groupBuy.setIntroduction(request.getIntroduction());
        groupBuy.setCoverImageUrl(request.getCoverImageUrl());
        groupBuy.setGroupType(groupType);

        String visibility = request.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            visibility = "public";
        } else if (!"public".equals(visibility) && !"hidden".equals(visibility)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购可见性只允许 public 或 hidden");
        }
        groupBuy.setVisibility(visibility);

        // Validate and save gallery image URLs
        contentValidationUtil.validateImageUrls(request.getGalleryImageUrls(),
                ContentValidationUtil.MAX_GALLERY_URLS, "galleryImageUrls");
        groupBuy.setGalleryImageUrls(contentValidationUtil.serializeImageUrls(request.getGalleryImageUrls()));

        // Validate and save content blocks
        contentValidationUtil.validateContentBlocks(request.getContentBlocks());
        List<ContentBlockData> blockDataList = contentValidationUtil.toContentBlockData(request.getContentBlocks());
        groupBuy.setContentBlocks(contentValidationUtil.serializeContentBlocks(blockDataList));

        groupBuy.setDeliveryType(request.getDeliveryType().getValue());
        groupBuy.setShippingTime(shippingTime);
        groupBuy.setStartTime(startTime);
        groupBuy.setEndTime(endTime);
        groupBuy.setStatus("published");
        groupBuyMapper.insert(groupBuy);
        registerGroupBuyImageReferences(groupBuy);

        List<GroupBuyItemData> itemResponses = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            ItemEntry entry = request.getItems().get(i);
            Long productId = resolveProductId(entry, store);
            validateItemEntry(entry);

            GroupBuyItem item = new GroupBuyItem();
            item.setGroupBuyId(groupBuy.getId());
            item.setProductId(productId);
            item.setDisplayName(entry.getDisplayName());
            item.setGroupPriceAmount(entry.getGroupPriceAmount());
            item.setGroupStock(entry.getGroupStock());
            item.setSoldCount(0);
            item.setSortOrder(entry.getSortOrder() != null ? entry.getSortOrder() : i);
            item.setShowStock(true);
            groupBuyItemMapper.insert(item);

            itemResponses.add(toItemData(item));
        }

        GroupBuyResponse response = GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(itemResponses)
                .build();
        notificationService.notifyGroupBuyPublished(groupBuy, userId);
        return response;
    }

    // ── List (my store) ───────────────────────────────────────────────

    public PageResponse<GroupBuyData> getMyStoreGroupBuys(Long userId, String status, int page, int pageSize) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        Page<GroupBuy> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupBuy> wrapper = new LambdaQueryWrapper<GroupBuy>()
                .eq(GroupBuy::getStoreId, store.getId())
                .orderByDesc(GroupBuy::getCreatedAt);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(GroupBuy::getStatus, status);
        }

        Page<GroupBuy> result = groupBuyMapper.selectPage(pageObj, wrapper);
        List<GroupBuyData> items = result.getRecords().stream()
                .map(this::toGroupBuyData)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    // ── Detail (my store) ─────────────────────────────────────────────

    public GroupBuyResponse getGroupBuy(Long userId, Long groupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());
        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(items.stream().map(this::toItemData).collect(Collectors.toList()))
                .build();
    }

    // ── Update ────────────────────────────────────────────────────────

    @Transactional
    public GroupBuyResponse updateGroupBuy(Long userId, Long groupBuyId, UpdateGroupBuyRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        boolean isDraft = "draft".equals(groupBuy.getStatus());

        // Ended/removed group buys cannot be edited at all
        if ("ended".equals(groupBuy.getStatus()) || "removed".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已结束或已移除的团购不可修改");
        }

        if (request.getTitle() != null) {
            groupBuy.setTitle(request.getTitle());
        }
        if (request.getIntroduction() != null) {
            groupBuy.setIntroduction(request.getIntroduction());
        }
        if (request.getCoverImageUrl() != null) {
            groupBuy.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getShippingTime() != null) {
            groupBuy.setShippingTime(parseIsoDateTime(request.getShippingTime()));
        }
        if (request.getStartTime() != null) {
            groupBuy.setStartTime(parseIsoDateTime(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            groupBuy.setEndTime(parseIsoDateTime(request.getEndTime()));
        }
        // Apply groupType — only allowed for drafts; published group buys cannot change type
        if (request.getGroupType() != null) {
            if (!isDraft) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已发布团购不可修改团购类型");
            }
            if (!"normal".equals(request.getGroupType()) && !"presale".equals(request.getGroupType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购类型只允许 normal 或 presale");
            }
            groupBuy.setGroupType(request.getGroupType());
        }
        // Apply visibility — allowed for drafts and published (unless ended/removed — handled by permission endpoint)
        if (request.getVisibility() != null) {
            if ("ended".equals(groupBuy.getStatus()) || "removed".equals(groupBuy.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已结束或已移除的团购不可修改可见性");
            }
            if (!"public".equals(request.getVisibility()) && !"hidden".equals(request.getVisibility())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购可见性只允许 public 或 hidden");
            }
            groupBuy.setVisibility(request.getVisibility());
        }

        // Update galleryImageUrls
        if (request.getGalleryImageUrls() != null) {
            contentValidationUtil.validateImageUrls(request.getGalleryImageUrls(), 9, "galleryImageUrls");
            groupBuy.setGalleryImageUrls(contentValidationUtil.serializeImageUrls(request.getGalleryImageUrls()));
        }

        // Update contentBlocks
        if (request.getContentBlocks() != null) {
            contentValidationUtil.validateContentBlocks(request.getContentBlocks());
            List<ContentBlockData> blockDataList = contentValidationUtil.toContentBlockData(request.getContentBlocks());
            groupBuy.setContentBlocks(contentValidationUtil.serializeContentBlocks(blockDataList));
        }

        // Validate time constraints (including presale rules)
        String resolvedGroupType = groupBuy.getGroupType();
        LocalDateTime start = groupBuy.getStartTime();
        LocalDateTime end = groupBuy.getEndTime();
        LocalDateTime shipping = groupBuy.getShippingTime();
        if ("presale".equals(resolvedGroupType)) {
            if (start != null && end != null && shipping != null) {
                if (!end.isAfter(start)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束时间必须晚于开始时间");
                }
                if (!shipping.isAfter(end)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "发货时间必须晚于结束时间");
                }
            }
            // partial updates: if only some times are set, skip full validation
        } else {
            validateEndTimeAfterStart(start, end);
        }
        groupBuyMapper.updateById(groupBuy);
        if (request.getCoverImageUrl() != null) {
            uploadAssetService.replaceReferences("group_buy", groupBuy.getId(), "coverImageUrl",
                    Collections.singletonList(groupBuy.getCoverImageUrl()));
        }
        if (request.getGalleryImageUrls() != null) {
            uploadAssetService.replaceReferences("group_buy", groupBuy.getId(), "galleryImageUrls",
                    contentValidationUtil.deserializeImageUrls(groupBuy.getGalleryImageUrls()));
        }
        if (request.getContentBlocks() != null) {
            uploadAssetService.replaceReferences("group_buy_content", groupBuy.getId(), "contentBlocks",
                    extractContentBlockImageUrls(groupBuy));
        }

        // ── Item updates ────────────────────────────────────────────
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            if (isDraft) {
                // Draft item replacement: read existing item productId map,
                // delete all, then insert new items
                List<GroupBuyItem> existingItems = groupBuyItemMapper.selectList(
                        new LambdaQueryWrapper<GroupBuyItem>()
                                .eq(GroupBuyItem::getGroupBuyId, groupBuyId));
                java.util.Map<Long, Long> existingProductIds = existingItems.stream()
                        .collect(Collectors.toMap(GroupBuyItem::getId, GroupBuyItem::getProductId));

                groupBuyItemMapper.delete(new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuyId));

                for (int i = 0; i < request.getItems().size(); i++) {
                    UpdateItemEntry itemEntry = request.getItems().get(i);

                    // Resolve productId: explicit > from existing item > error
                    Long productId = itemEntry.getProductId();
                    if (productId == null && itemEntry.getId() != null) {
                        productId = existingProductIds.get(itemEntry.getId());
                    }
                    if (productId == null) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                "每个团购商品必须指定 productId 或引用已有商品 id");
                    }
                    // Validate the product exists, belongs to this store, and is not deleted
                    Product product = productMapper.selectById(productId);
                    if (product == null) {
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
                    }
                    if ("deleted".equals(product.getStatus())) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品已被删除");
                    }
                    if (!product.getStoreId().equals(store.getId())) {
                        throw new BusinessException(ErrorCode.STORE_FORBIDDEN, "商品不属于当前店铺");
                    }

                    GroupBuyItem newItem = new GroupBuyItem();
                    newItem.setGroupBuyId(groupBuyId);
                    newItem.setProductId(productId);
                    newItem.setDisplayName(itemEntry.getDisplayName() != null
                            ? itemEntry.getDisplayName() : "商品");
                    newItem.setGroupPriceAmount(itemEntry.getGroupPriceAmount() != null
                            ? itemEntry.getGroupPriceAmount() : 0);
                    newItem.setGroupStock(itemEntry.getGroupStock() != null
                            ? itemEntry.getGroupStock() : 0);
                    newItem.setSoldCount(0);
                    newItem.setSortOrder(itemEntry.getSortOrder() != null ? itemEntry.getSortOrder() : i);
                    newItem.setShowStock(true);
                    groupBuyItemMapper.insert(newItem);
                }
            } else {
                // Published group buy: update existing items by ID with price protection
                for (UpdateItemEntry itemEntry : request.getItems()) {
                    GroupBuyItem existingItem = groupBuyItemMapper.selectById(itemEntry.getId());
                    if (existingItem == null || !existingItem.getGroupBuyId().equals(groupBuyId)) {
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购商品不存在");
                    }

                    boolean hasOrders = orderItemMapper.selectCount(
                            new LambdaQueryWrapper<OrderItem>()
                                    .eq(OrderItem::getGroupBuyItemId, existingItem.getId())) > 0;

                    if (hasOrders && itemEntry.getGroupPriceAmount() != null
                            && !itemEntry.getGroupPriceAmount().equals(existingItem.getGroupPriceAmount())) {
                        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                                "团购商品已产生订单，不允许修改价格");
                    }

                    if (itemEntry.getDisplayName() != null) {
                        existingItem.setDisplayName(itemEntry.getDisplayName());
                    }
                    if (itemEntry.getGroupPriceAmount() != null) {
                        existingItem.setGroupPriceAmount(itemEntry.getGroupPriceAmount());
                    }
                    if (itemEntry.getGroupStock() != null) {
                        if (itemEntry.getGroupStock() < existingItem.getSoldCount()) {
                            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                                    "库存不能小于已售数量");
                        }
                        existingItem.setGroupStock(itemEntry.getGroupStock());
                    }
                    if (itemEntry.getSortOrder() != null) {
                        existingItem.setSortOrder(itemEntry.getSortOrder());
                    }
                    groupBuyItemMapper.updateById(existingItem);
                }
            }
        }

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(items.stream().map(this::toItemData).collect(Collectors.toList()))
                .build();
    }

    // ── End ───────────────────────────────────────────────────────────

    @Transactional
    public GroupBuyResponse endGroupBuy(Long userId, Long groupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只有发布状态的团购可以结束");
        }

        groupBuy.setStatus("ended");
        groupBuyMapper.updateById(groupBuy);

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(items.stream().map(this::toItemData).collect(Collectors.toList()))
                .build();
    }

    // ── Draft ─────────────────────────────────────────────────────────

    /**
     * Create a group buy draft with minimal validation.
     * Status is set to "draft".
     */
    @Transactional
    public GroupBuyResponse createDraft(Long userId, CreateDraftGroupBuyRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();
        Leader leader = ls.getLeader();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购至少包含一个商品");
        }

        String groupType = request.getGroupType();
        if (groupType == null || groupType.isBlank()) {
            groupType = "normal";
        } else if (!"normal".equals(groupType) && !"presale".equals(groupType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购类型只允许 normal 或 presale");
        }

        String visibility = request.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            visibility = "public";
        } else if (!"public".equals(visibility) && !"hidden".equals(visibility)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购可见性只允许 public 或 hidden");
        }

        // Validate deliveryType
        String deliveryType = request.getDeliveryType();
        if (!isValidDeliveryType(deliveryType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "配送方式只支持 express / pickup / local_delivery");
        }

        GroupBuy groupBuy = new GroupBuy();
        groupBuy.setStoreId(store.getId());
        groupBuy.setLeaderId(leader.getId());
        groupBuy.setTitle(request.getTitle());
        groupBuy.setIntroduction(request.getIntroduction());
        groupBuy.setCoverImageUrl(request.getCoverImageUrl());

        // Validate and save gallery image URLs
        contentValidationUtil.validateImageUrls(request.getGalleryImageUrls(), 9, "galleryImageUrls");
        groupBuy.setGalleryImageUrls(contentValidationUtil.serializeImageUrls(request.getGalleryImageUrls()));

        // Validate and save content blocks
        contentValidationUtil.validateContentBlocks(request.getContentBlocks());
        List<ContentBlockData> blockDataList = contentValidationUtil.toContentBlockData(request.getContentBlocks());
        groupBuy.setContentBlocks(contentValidationUtil.serializeContentBlocks(blockDataList));

        groupBuy.setGroupType(groupType);
        groupBuy.setVisibility(visibility);
        groupBuy.setDeliveryType(deliveryType);
        groupBuy.setShippingTime(parseIsoDateTime(request.getShippingTime()));
        groupBuy.setStartTime(parseIsoDateTime(request.getStartTime()));
        groupBuy.setEndTime(parseIsoDateTime(request.getEndTime()));
        groupBuy.setStatus("draft");
        groupBuyMapper.insert(groupBuy);
        registerGroupBuyImageReferences(groupBuy);

        List<GroupBuyItemData> itemResponses = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateDraftGroupBuyRequest.ItemEntry entry = request.getItems().get(i);
            Long productId = resolveDraftProductId(entry, store);

            GroupBuyItem item = new GroupBuyItem();
            item.setGroupBuyId(groupBuy.getId());
            item.setProductId(productId);
            item.setDisplayName(entry.getDisplayName());
            item.setGroupPriceAmount(entry.getGroupPriceAmount() != null ? entry.getGroupPriceAmount() : 0);
            item.setGroupStock(entry.getGroupStock() != null ? entry.getGroupStock() : 0);
            item.setSoldCount(0);
            item.setSortOrder(entry.getSortOrder() != null ? entry.getSortOrder() : i);
            item.setShowStock(true);
            groupBuyItemMapper.insert(item);

            itemResponses.add(toItemData(item));
        }

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(itemResponses)
                .build();
    }

    // ── Publish ────────────────────────────────────────────────────────

    /**
     * Publish a draft group buy. Validates publish-readiness:
     * - title, deliveryType, items are already required
     * - For presale: startTime, endTime, shippingTime are required with correct ordering
     */
    @Transactional
    public GroupBuyResponse publishGroupBuy(Long userId, Long groupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        if (!"draft".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只有草稿状态的团购可以发布");
        }

        // Validate publish-readiness
        if (groupBuy.getTitle() == null || groupBuy.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "标题不能为空");
        }
        if (!isValidDeliveryType(groupBuy.getDeliveryType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "配送方式只支持 express / pickup / local_delivery");
        }

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId()));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购至少包含一个商品");
        }

        // Validate item prices and stock are non-negative
        for (GroupBuyItem item : items) {
            if (item.getGroupPriceAmount() == null || item.getGroupPriceAmount() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "团购商品 \"" + item.getDisplayName() + "\" 的团购价不能为负数");
            }
            if (item.getGroupStock() == null || item.getGroupStock() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "团购商品 \"" + item.getDisplayName() + "\" 的库存不能为负数");
            }
        }

        // Presale validation
        if ("presale".equals(groupBuy.getGroupType())) {
            if (groupBuy.getStartTime() == null || groupBuy.getEndTime() == null || groupBuy.getShippingTime() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "预售团购必须指定 startTime、endTime 和 shippingTime");
            }
            if (!groupBuy.getEndTime().isAfter(groupBuy.getStartTime())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束时间必须晚于开始时间");
            }
            if (!groupBuy.getShippingTime().isAfter(groupBuy.getEndTime())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "发货时间必须晚于结束时间");
            }
        } else {
            validateEndTimeAfterStart(groupBuy.getStartTime(), groupBuy.getEndTime());
        }

        groupBuy.setStatus("published");
        groupBuyMapper.updateById(groupBuy);
        notificationService.notifyGroupBuyPublished(groupBuy, userId);

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(items.stream().map(this::toItemData).collect(Collectors.toList()))
                .build();
    }

    // ── Preview ────────────────────────────────────────────────────────

    /**
     * Preview a group buy (any status) for the owning leader.
     * Returns the same detail structure as the public detail endpoint.
     */
    public GroupBuyDetailResponse previewGroupBuy(Long userId, Long groupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        Leader leader = leaderMapper.selectById(groupBuy.getLeaderId());
        Store storeDetail = storeMapper.selectById(groupBuy.getStoreId());

        List<GroupBuyDetailItemData> detailItems = items.stream()
                .map(this::toGroupBuyDetailItemData)
                .collect(Collectors.toList());

        LeaderDetail ld = LeaderDetail.builder()
                .id(leader.getId())
                .displayName(leader.getDisplayName())
                .avatarUrl(leader.getAvatarUrl())
                .followerCount(leader.getFollowerCount())
                .build();

        return GroupBuyDetailResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .leader(ld)
                .store(StoreDetail.builder()
                        .id(storeDetail.getId())
                        .name(storeDetail.getName())
                        .logoUrl(storeDetail.getLogoUrl())
                        .build())
                .items(detailItems)
                .featuredItem(selectFeaturedItem(detailItems))
                .viewer(new ViewerInfo(false, false))
                .build();
    }

    // ── Copy ──────────────────────────────────────────────────────────

    /**
     * Copy a group buy as a new draft.
     * Copies title, introduction, coverImageUrl, galleryImageUrls, contentBlocks,
     * groupType, deliveryType, visibility, and all item configurations.
     * New item IDs are generated; soldCount is reset to 0.
     * The original group buy is not affected.
     */
    @Transactional
    public GroupBuyResponse copyGroupBuy(Long userId, Long sourceGroupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();
        Leader leader = ls.getLeader();

        GroupBuy source = findGroupBuyForStore(sourceGroupBuyId, store.getId());

        // Create new draft from source
        GroupBuy draft = new GroupBuy();
        draft.setStoreId(store.getId());
        draft.setLeaderId(leader.getId());
        draft.setTitle(source.getTitle());
        draft.setIntroduction(source.getIntroduction());
        draft.setCoverImageUrl(source.getCoverImageUrl());
        draft.setGalleryImageUrls(source.getGalleryImageUrls());
        draft.setContentBlocks(source.getContentBlocks());
        draft.setGroupType(source.getGroupType());
        draft.setDeliveryType(source.getDeliveryType());
        draft.setShippingTime(null);        // Don't copy times — buyer must set fresh
        draft.setStartTime(null);
        draft.setEndTime(null);
        draft.setVisibility(source.getVisibility());
        draft.setStatus("draft");
        groupBuyMapper.insert(draft);
        registerGroupBuyImageReferences(draft);

        // Copy items with new IDs and reset soldCount
        List<GroupBuyItem> sourceItems = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, source.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        List<GroupBuyItemData> itemResponses = new ArrayList<>();
        for (int i = 0; i < sourceItems.size(); i++) {
            GroupBuyItem srcItem = sourceItems.get(i);

            GroupBuyItem newItem = new GroupBuyItem();
            newItem.setGroupBuyId(draft.getId());
            newItem.setProductId(srcItem.getProductId());
            newItem.setDisplayName(srcItem.getDisplayName());
            newItem.setGroupPriceAmount(srcItem.getGroupPriceAmount());
            newItem.setGroupStock(srcItem.getGroupStock());
            newItem.setSoldCount(0);
            newItem.setSortOrder(srcItem.getSortOrder() != null ? srcItem.getSortOrder() : i);
            newItem.setShowStock(srcItem.getShowStock());
            groupBuyItemMapper.insert(newItem);

            itemResponses.add(toItemData(newItem));
        }

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(draft))
                .items(itemResponses)
                .build();
    }

    // ── Permission ──────────────────────────────────────────────────────

    /**
     * Update a group buy's visibility. Only published/draft/hidden group buys
     * can have their visibility changed. Ended/removed group buys are rejected.
     */
    @Transactional
    public GroupBuyResponse updatePermission(Long userId, Long groupBuyId, UpdateGroupBuyPermissionRequest request) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        if ("ended".equals(groupBuy.getStatus()) || "removed".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已结束或已移除的团购不可修改权限");
        }

        String visibility = request.getVisibility();
        if (!"public".equals(visibility) && !"hidden".equals(visibility)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购可见性只允许 public 或 hidden");
        }

        groupBuy.setVisibility(visibility);
        groupBuyMapper.updateById(groupBuy);

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(items.stream().map(this::toItemData).collect(Collectors.toList()))
                .build();
    }

    // ── Share Card / Token ──────────────────────────────────────────────

    /**
     * Get or create an active share token for a group buy.
     * If an active token already exists, it is reused (not regenerated).
     */
    public ShareCardResponse getOrCreateShareToken(Long userId, Long groupBuyId) {
        var ls = currentStoreHelper.getLeaderAndStore(userId);
        Store store = ls.getStore();

        GroupBuy groupBuy = findGroupBuyForStore(groupBuyId, store.getId());

        // Only published group buys can have share tokens
        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只有已发布的团购可以生成分享链接");
        }

        // Check for existing active token
        GroupBuyShareToken existing = groupBuyShareTokenMapper.selectOne(
                new LambdaQueryWrapper<GroupBuyShareToken>()
                        .eq(GroupBuyShareToken::getGroupBuyId, groupBuy.getId())
                        .eq(GroupBuyShareToken::getStatus, "active"));

        if (existing != null) {
            // Check if still valid by expiresAt
            if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(LocalDateTime.now())) {
                // Expired — deactivate and create new
                existing.setStatus("expired");
                groupBuyShareTokenMapper.updateById(existing);
                existing = null;
            } else {
                return buildShareCardResponse(groupBuy, existing.getToken());
            }
        }

        // Create new token
        GroupBuyShareToken token = new GroupBuyShareToken();
        token.setGroupBuyId(groupBuy.getId());
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setStatus("active");
        token.setExpiresAt(null); // long-lived
        groupBuyShareTokenMapper.insert(token);

        return buildShareCardResponse(groupBuy, token.getToken());
    }

    /**
     * Validate a share token and return the group buy ID it grants access to.
     * Throws if token is invalid, expired, revoked, or missing.
     */
    public GroupBuy validateShareToken(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享 token 不能为空");
        }

        GroupBuyShareToken token = groupBuyShareTokenMapper.selectOne(
                new LambdaQueryWrapper<GroupBuyShareToken>()
                        .eq(GroupBuyShareToken::getToken, shareToken));

        if (token == null || !"active".equals(token.getStatus())) {
            throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享链接无效或已过期");
        }

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SHARE_TOKEN_INVALID, "分享链接已过期");
        }

        GroupBuy groupBuy = groupBuyMapper.selectById(token.getGroupBuyId());
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "团购不存在");
        }
        if (!"published".equals(groupBuy.getStatus())) {
            throw new BusinessException(ErrorCode.GROUP_BUY_NOT_PURCHASABLE, "团购不可购买");
        }
        return groupBuy;
    }

    /**
     * Get public group buy detail via share token.
     * This allows access to hidden group buys that have a valid share token.
     */
    public GroupBuyDetailResponse getPublicGroupBuyDetailByShareToken(String shareToken, Long viewerUserId) {
        GroupBuy groupBuy = validateShareToken(shareToken);

        // Record browsing history
        if (viewerUserId != null) {
            browsingHistoryService.recordView(viewerUserId, groupBuy.getId());
        }

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        Leader leader = leaderMapper.selectById(groupBuy.getLeaderId());
        Store storeDetail = storeMapper.selectById(groupBuy.getStoreId());

        boolean subscribed = viewerUserId != null
                && subscriptionService.isSubscribed(viewerUserId, groupBuy.getLeaderId());
        boolean favorited = favoriteService.isFavorited(viewerUserId, groupBuy.getId());
        List<GroupBuyDetailItemData> detailItems = items.stream()
                .map(this::toGroupBuyDetailItemData)
                .collect(Collectors.toList());

        return GroupBuyDetailResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .leader(LeaderDetail.builder()
                        .id(leader.getId())
                        .displayName(leader.getDisplayName())
                        .avatarUrl(leader.getAvatarUrl())
                        .followerCount(leader.getFollowerCount())
                        .build())
                .store(buildStoreDetailWithDistance(storeDetail, null, null))
                .items(detailItems)
                .featuredItem(selectFeaturedItem(detailItems))
                .viewer(new ViewerInfo(subscribed, favorited))
                .build();
    }

    /**
     * Get public group buy detail via share token, with optional user location.
     */
    public GroupBuyDetailResponse getPublicGroupBuyDetailByShareToken(String shareToken, Long viewerUserId,
                                                                       BigDecimal latitude, BigDecimal longitude) {
        GroupBuy groupBuy = validateShareToken(shareToken);

        // Record browsing history
        if (viewerUserId != null) {
            browsingHistoryService.recordView(viewerUserId, groupBuy.getId());
        }

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        Leader leader = leaderMapper.selectById(groupBuy.getLeaderId());
        Store storeDetail = storeMapper.selectById(groupBuy.getStoreId());

        boolean subscribed = viewerUserId != null
                && subscriptionService.isSubscribed(viewerUserId, groupBuy.getLeaderId());
        boolean favorited = favoriteService.isFavorited(viewerUserId, groupBuy.getId());
        List<GroupBuyDetailItemData> detailItems = items.stream()
                .map(this::toGroupBuyDetailItemData)
                .collect(Collectors.toList());

        return GroupBuyDetailResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .leader(LeaderDetail.builder()
                        .id(leader.getId())
                        .displayName(leader.getDisplayName())
                        .avatarUrl(leader.getAvatarUrl())
                        .followerCount(leader.getFollowerCount())
                        .build())
                .store(buildStoreDetailWithDistance(storeDetail, latitude, longitude))
                .items(detailItems)
                .featuredItem(selectFeaturedItem(detailItems))
                .viewer(new ViewerInfo(subscribed, favorited))
                .build();
    }

    // ── Public browsing ───────────────────────────────────────────────

    /**
     * List public published group buys with optional keyword and categoryId filtering.
     * Public lists do NOT return visibility.
     */
    public PageResponse<PublicGroupBuyItem> getPublicGroupBuys(int page, int pageSize,
                                                                String keyword, Long categoryId) {
        return getPublicGroupBuys(page, pageSize, keyword, categoryId,
                null, null, null, null);
    }

    /**
     * List public published group buys with optional location-based filtering and sorting.
     *
     * <p>When user coordinates are provided, distance is computed for each store.
     * {@code maxDistanceMeters} filters out stores beyond the given distance.
     * {@code sort=distance} sorts results by distance ascending.
     * Distance filtering/sorting happens after keyword/category filtering but before pagination.
     */
    public PageResponse<PublicGroupBuyItem> getPublicGroupBuys(int page, int pageSize,
                                                                String keyword, Long categoryId,
                                                                BigDecimal latitude, BigDecimal longitude,
                                                                Long maxDistanceMeters, String sort) {
        // Step 1: Apply base filters (status, visibility, keyword, categoryId)
        LambdaQueryWrapper<GroupBuy> wrapper = new LambdaQueryWrapper<GroupBuy>()
                .eq(GroupBuy::getStatus, "published")
                .eq(GroupBuy::getVisibility, "public")
                .orderByDesc(GroupBuy::getCreatedAt);

        boolean hasLocation = latitude != null && longitude != null;

        // Keyword filtering
        if (keyword != null && !keyword.isBlank()) {
            Set<Long> keywordMatchIds = new HashSet<>();

            List<GroupBuy> titleMatches = groupBuyMapper.selectList(
                    new LambdaQueryWrapper<GroupBuy>()
                            .like(GroupBuy::getTitle, keyword)
                            .or().like(GroupBuy::getIntroduction, keyword)
                            .eq(GroupBuy::getStatus, "published")
                            .eq(GroupBuy::getVisibility, "public"));
            titleMatches.forEach(gb -> keywordMatchIds.add(gb.getId()));

            List<GroupBuyItem> itemMatches = groupBuyItemMapper.selectList(
                    new LambdaQueryWrapper<GroupBuyItem>()
                            .like(GroupBuyItem::getDisplayName, keyword));
            itemMatches.forEach(item -> keywordMatchIds.add(item.getGroupBuyId()));

            List<Product> productMatches = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .like(Product::getName, keyword));
            if (!productMatches.isEmpty()) {
                Set<Long> productIds = productMatches.stream().map(Product::getId).collect(Collectors.toSet());
                List<GroupBuyItem> productItemMatches = groupBuyItemMapper.selectList(
                        new LambdaQueryWrapper<GroupBuyItem>()
                                .in(GroupBuyItem::getProductId, productIds));
                productItemMatches.forEach(item -> keywordMatchIds.add(item.getGroupBuyId()));
            }

            if (keywordMatchIds.isEmpty()) {
                return PageResponse.of(List.of(), page, pageSize, 0);
            }
            wrapper.in(GroupBuy::getId, keywordMatchIds);
        }

        // CategoryId filtering
        if (categoryId != null) {
            List<Product> catProducts = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, categoryId));
            if (catProducts.isEmpty()) {
                return PageResponse.of(List.of(), page, pageSize, 0);
            }
            Set<Long> catProductIds = catProducts.stream().map(Product::getId).collect(Collectors.toSet());
            List<GroupBuyItem> catItems = groupBuyItemMapper.selectList(
                    new LambdaQueryWrapper<GroupBuyItem>()
                            .in(GroupBuyItem::getProductId, catProductIds));
            if (catItems.isEmpty()) {
                return PageResponse.of(List.of(), page, pageSize, 0);
            }
            Set<Long> catGbIds = catItems.stream().map(GroupBuyItem::getGroupBuyId).collect(Collectors.toSet());
            wrapper.in(GroupBuy::getId, catGbIds);
        }

        // Fetch all matching group buys (unpaginated) for distance computation
        List<GroupBuy> allMatching = groupBuyMapper.selectList(wrapper);

        if (allMatching.isEmpty()) {
            return PageResponse.of(List.of(), page, pageSize, 0);
        }

        // Step 2: Convert to items, optionally with distance
        List<PublicGroupBuyItem> allItems = allMatching.stream()
                .map(gb -> toPublicGroupBuyItem(gb, latitude, longitude))
                .collect(Collectors.toList());

        // Step 3: Apply distance filter
        if (hasLocation && maxDistanceMeters != null && maxDistanceMeters > 0) {
            allItems = allItems.stream()
                    .filter(item -> item.getStore().getDistanceMeters() != null
                            && item.getStore().getDistanceMeters() <= maxDistanceMeters)
                    .collect(Collectors.toList());
        }

        // Step 4: Apply sorting
        boolean sortByDistance = "distance".equals(sort) && hasLocation;
        if (sortByDistance) {
            allItems.sort((a, b) -> {
                Long d1 = a.getStore().getDistanceMeters();
                Long d2 = b.getStore().getDistanceMeters();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return Long.compare(d1, d2);
            });
        }
        // Default order (not distance): the SQL query already returns
        // newest-first via orderByDesc(GroupBuy::getCreatedAt) on the wrapper,
        // and allItems preserves that order from allMatching.

        // Step 5: Manual pagination
        int total = allItems.size();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= total) {
            return PageResponse.of(List.of(), page, pageSize, total);
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<PublicGroupBuyItem> pageItems = allItems.subList(fromIndex, toIndex);

        return PageResponse.of(pageItems, page, pageSize, total);
    }

    /**
     * Get public group buy detail.
     * Returns visibility in groupBuy.visibility (per MVP contract).
     * Shows viewer.favorited for authenticated users.
     * Records browsing history for authenticated users (auxiliary, failure logged only).
     */
    public GroupBuyDetailResponse getPublicGroupBuyDetail(Long groupBuyId, Long viewerUserId) {
        return getPublicGroupBuyDetail(groupBuyId, viewerUserId, null, null);
    }

    /**
     * Get public group buy detail with optional user location for distance display.
     */
    public GroupBuyDetailResponse getPublicGroupBuyDetail(Long groupBuyId, Long viewerUserId,
                                                           BigDecimal latitude, BigDecimal longitude) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null || !"published".equals(groupBuy.getStatus()) || !"public".equals(groupBuy.getVisibility())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // Record browsing history (auxiliary, failure swallowed)
        if (viewerUserId != null) {
            browsingHistoryService.recordView(viewerUserId, groupBuyId);
        }

        List<GroupBuyItem> items = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, groupBuy.getId())
                        .orderByAsc(GroupBuyItem::getSortOrder));

        Leader leader = leaderMapper.selectById(groupBuy.getLeaderId());
        Store store = storeMapper.selectById(groupBuy.getStoreId());

        LeaderDetail leaderDetail = LeaderDetail.builder()
                .id(leader.getId())
                .displayName(leader.getDisplayName())
                .avatarUrl(leader.getAvatarUrl())
                .followerCount(leader.getFollowerCount())
                .build();

        StoreDetail storeDetail = buildStoreDetailWithDistance(store, latitude, longitude);

        boolean subscribed = viewerUserId != null
                && subscriptionService.isSubscribed(viewerUserId, groupBuy.getLeaderId());

        boolean favorited = favoriteService.isFavorited(viewerUserId, groupBuyId);

        List<GroupBuyDetailItemData> detailItems = items.stream()
                .map(this::toGroupBuyDetailItemData)
                .collect(Collectors.toList());

        return GroupBuyDetailResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .leader(leaderDetail)
                .store(storeDetail)
                .items(detailItems)
                .featuredItem(selectFeaturedItem(detailItems))
                .viewer(new ViewerInfo(subscribed, favorited))
                .build();
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private Long resolveProductId(ItemEntry entry, Store store) {
        if (entry.getProductId() != null && entry.getProduct() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每个团购商品只能指定 productId 或 product 之一，不能同时指定");
        }
        if (entry.getProductId() != null) {
            Product product = productMapper.selectById(entry.getProductId());
            if (product == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
            }
            if (!product.getStoreId().equals(store.getId())) {
                throw new BusinessException(ErrorCode.STORE_FORBIDDEN, "商品不属于当前店铺");
            }
            if ("deleted".equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品已被删除");
            }
            return product.getId();
        } else if (entry.getProduct() != null) {
            // Inline-create a new product (categoryId is allowed to be null)
            InlineProduct inlineProduct = entry.getProduct();
            Product product = new Product();
            product.setStoreId(store.getId());
            product.setName(inlineProduct.getName());
            product.setDescription(inlineProduct.getDescription());
            product.setCoverImageUrl(inlineProduct.getCoverImageUrl());
            contentValidationUtil.validateImageUrls(inlineProduct.getDetailImageUrls(), 9, "inlineProduct.detailImageUrls");
            product.setDetailImageUrls(contentValidationUtil.serializeImageUrls(inlineProduct.getDetailImageUrls()));
            product.setBasePriceAmount(inlineProduct.getBasePriceAmount());
            product.setStock(inlineProduct.getStock());
            product.setStatus("active");
            productMapper.insert(product);
            registerInlineProductImageReferences(product);
            return product.getId();
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每个团购商品必须指定 productId 或 product");
        }
    }

    private void validateItemEntry(ItemEntry entry) {
        if (entry.getGroupStock() != null && entry.getGroupStock() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "团购库存不能为负数");
        }
    }

    private GroupBuy findGroupBuyForStore(Long groupBuyId, Long storeId) {
        GroupBuy groupBuy = groupBuyMapper.selectById(groupBuyId);
        if (groupBuy == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!groupBuy.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.STORE_FORBIDDEN);
        }
        return groupBuy;
    }

    private static LocalDateTime parseIsoDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toLocalDateTime();
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "时间格式不合法: " + dateTimeStr);
        }
    }

    private static void validateEndTimeAfterStart(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束时间必须晚于开始时间");
        }
    }

    private GroupBuyDetailItemData toGroupBuyDetailItemData(GroupBuyItem item) {
        Product product = productMapper.selectById(item.getProductId());
        GroupBuyDetailItemData.GroupBuyDetailItemDataBuilder builder = GroupBuyDetailItemData.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .displayName(item.getDisplayName())
                .groupPriceAmount(item.getGroupPriceAmount())
                .groupStock(item.getGroupStock())
                .soldCount(item.getSoldCount())
                .sortOrder(item.getSortOrder())
                .coverImageUrl(product != null ? product.getCoverImageUrl() : null);
        if (product != null) {
            builder.product(toProductSummary(product));
        }
        return builder.build();
    }

    // ── Converters ────────────────────────────────────────────────────

    public PublicGroupBuyItem toPublicGroupBuyItem(GroupBuy gb) {
        return toPublicGroupBuyItem(gb, null, null);
    }

    /**
     * Convert a GroupBuy to a PublicGroupBuyItem, optionally computing distance
     * from the user's location.
     */
    public PublicGroupBuyItem toPublicGroupBuyItem(GroupBuy gb,
                                                    BigDecimal userLat, BigDecimal userLon) {
        List<GroupBuyItem> gbItems = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, gb.getId()));

        long minPriceAmount = gbItems.stream()
                .mapToLong(GroupBuyItem::getGroupPriceAmount)
                .min().orElse(0);

        int soldCount = gbItems.stream()
                .mapToInt(GroupBuyItem::getSoldCount)
                .sum();

        Leader leader = leaderMapper.selectById(gb.getLeaderId());
        Store store = storeMapper.selectById(gb.getStoreId());

        LeaderLite leaderLite = LeaderLite.builder()
                .id(leader.getId())
                .displayName(leader.getDisplayName())
                .avatarUrl(leader.getAvatarUrl())
                .build();

        StoreLite storeLite = buildStoreLiteWithDistance(store, userLat, userLon);

        return PublicGroupBuyItem.builder()
                .id(gb.getId())
                .title(gb.getTitle())
                .coverImageUrl(gb.getCoverImageUrl())
                .status(gb.getStatus())
                .groupType(gb.getGroupType())
                .startTime(gb.getStartTime() != null ? gb.getStartTime().toString() : null)
                .endTime(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                .shippingTime(gb.getShippingTime() != null ? gb.getShippingTime().toString() : null)
                .minPriceAmount(minPriceAmount)
                .soldCount(soldCount)
                .leader(leaderLite)
                .store(storeLite)
                .build();
    }

    /**
     * Build a StoreLite with distance information from user location.
     */
    private StoreLite buildStoreLiteWithDistance(Store store, BigDecimal userLat, BigDecimal userLon) {
        Long distanceMeters = DistanceCalculator.haversineMeters(
                userLat, userLon, store.getLatitude(), store.getLongitude());
        return StoreLite.builder()
                .id(store.getId())
                .name(store.getName())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .distanceMeters(distanceMeters)
                .distanceText(DistanceCalculator.formatDistance(distanceMeters))
                .build();
    }

    /**
     * Build a StoreDetail with distance information from user location.
     */
    private StoreDetail buildStoreDetailWithDistance(Store store, BigDecimal userLat, BigDecimal userLon) {
        Long distanceMeters = DistanceCalculator.haversineMeters(
                userLat, userLon, store.getLatitude(), store.getLongitude());
        return StoreDetail.builder()
                .id(store.getId())
                .name(store.getName())
                .logoUrl(store.getLogoUrl())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .distanceMeters(distanceMeters)
                .distanceText(DistanceCalculator.formatDistance(distanceMeters))
                .build();
    }

    private GroupBuyData toGroupBuyData(GroupBuy gb) {
        return GroupBuyData.builder()
                .id(gb.getId())
                .storeId(gb.getStoreId())
                .leaderId(gb.getLeaderId())
                .title(gb.getTitle())
                .introduction(gb.getIntroduction())
                .coverImageUrl(gb.getCoverImageUrl())
                .groupType(gb.getGroupType())
                .deliveryType(gb.getDeliveryType())
                .shippingTime(gb.getShippingTime() != null ? gb.getShippingTime().toString() : null)
                .startTime(gb.getStartTime() != null ? gb.getStartTime().toString() : null)
                .endTime(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                .visibility(gb.getVisibility())
                .status(gb.getStatus())
                .galleryImageUrls(contentValidationUtil.deserializeImageUrls(gb.getGalleryImageUrls()))
                .contentBlocks(contentValidationUtil.deserializeContentBlocks(gb.getContentBlocks()))
                .build();
    }

    private GroupBuyItemData toItemData(GroupBuyItem item) {
        GroupBuyItemData.GroupBuyItemDataBuilder builder = GroupBuyItemData.builder()
                .id(item.getId())
                .groupBuyId(item.getGroupBuyId())
                .productId(item.getProductId())
                .displayName(item.getDisplayName())
                .groupPriceAmount(item.getGroupPriceAmount())
                .groupStock(item.getGroupStock())
                .soldCount(item.getSoldCount())
                .sortOrder(item.getSortOrder());

        Product product = productMapper.selectById(item.getProductId());
        if (product != null) {
            builder.productName(product.getName())
                    .productCoverImageUrl(product.getCoverImageUrl())
                    .productBasePriceAmount(product.getBasePriceAmount())
                    .productStatus(product.getStatus());
        }

        return builder.build();
    }

    private void registerGroupBuyImageReferences(GroupBuy groupBuy) {
        uploadAssetService.registerReferences("group_buy", groupBuy.getId(), "coverImageUrl",
                Collections.singletonList(groupBuy.getCoverImageUrl()));
        uploadAssetService.registerReferences("group_buy", groupBuy.getId(), "galleryImageUrls",
                contentValidationUtil.deserializeImageUrls(groupBuy.getGalleryImageUrls()));
        uploadAssetService.registerReferences("group_buy_content", groupBuy.getId(), "contentBlocks",
                extractContentBlockImageUrls(groupBuy));
    }

    private List<String> extractContentBlockImageUrls(GroupBuy groupBuy) {
        return contentValidationUtil.deserializeContentBlocks(groupBuy.getContentBlocks()).stream()
                .filter(block -> "image".equals(block.getType()))
                .map(ContentBlockData::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());
    }

    private void registerInlineProductImageReferences(Product product) {
        uploadAssetService.registerReferences("product", product.getId(), "coverImageUrl",
                Collections.singletonList(product.getCoverImageUrl()));
        uploadAssetService.registerReferences("product", product.getId(), "detailImageUrls",
                contentValidationUtil.deserializeImageUrls(product.getDetailImageUrls()));
    }

    // ── Share card helper ───────────────────────────────────────────

    private ShareCardResponse buildShareCardResponse(GroupBuy gb, String shareToken) {
        List<GroupBuyItem> gbItems = groupBuyItemMapper.selectList(
                new LambdaQueryWrapper<GroupBuyItem>()
                        .eq(GroupBuyItem::getGroupBuyId, gb.getId()));

        long minPrice = gbItems.stream().mapToLong(GroupBuyItem::getGroupPriceAmount).min().orElse(0);
        long maxPrice = gbItems.stream().mapToLong(GroupBuyItem::getGroupPriceAmount).max().orElse(0);

        Leader leader = leaderMapper.selectById(gb.getLeaderId());
        Store store = storeMapper.selectById(gb.getStoreId());

        return ShareCardResponse.builder()
                .shareToken(shareToken)
                .landingPath("/share/group-buys/" + shareToken)
                .groupBuyId(gb.getId())
                .title(gb.getTitle())
                .coverImageUrl(gb.getCoverImageUrl())
                .minPriceAmount(minPrice)
                .maxPriceAmount(maxPrice)
                .endTime(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                .groupType(gb.getGroupType())
                .storeId(store != null ? store.getId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeLogoUrl(store != null ? store.getLogoUrl() : null)
                .leaderId(leader != null ? leader.getId() : null)
                .leaderName(leader != null ? leader.getDisplayName() : null)
                .leaderAvatarUrl(leader != null ? leader.getAvatarUrl() : null)
                .deliveryType(gb.getDeliveryType())
                .shippingTime(gb.getShippingTime() != null ? gb.getShippingTime().toString() : null)
                .build();
    }

    // ── Draft product resolver ──────────────────────────────────────

    private Long resolveDraftProductId(CreateDraftGroupBuyRequest.ItemEntry entry, Store store) {
        if (entry.getProductId() != null && entry.getProduct() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每个团购商品只能指定 productId 或 product 之一，不能同时指定");
        }
        if (entry.getProductId() != null) {
            Product product = productMapper.selectById(entry.getProductId());
            if (product == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
            }
            if (!product.getStoreId().equals(store.getId())) {
                throw new BusinessException(ErrorCode.STORE_FORBIDDEN, "商品不属于当前店铺");
            }
            if ("deleted".equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品已被删除");
            }
            return product.getId();
        } else if (entry.getProduct() != null) {
            CreateDraftGroupBuyRequest.InlineProduct inlineProduct = entry.getProduct();
            Product product = new Product();
            product.setStoreId(store.getId());
            product.setName(inlineProduct.getName());
            product.setDescription(inlineProduct.getDescription());
            product.setCoverImageUrl(inlineProduct.getCoverImageUrl());
            contentValidationUtil.validateImageUrls(inlineProduct.getDetailImageUrls(), 9, "inlineProduct.detailImageUrls");
            product.setDetailImageUrls(contentValidationUtil.serializeImageUrls(inlineProduct.getDetailImageUrls()));
            product.setBasePriceAmount(inlineProduct.getBasePriceAmount() != null ? inlineProduct.getBasePriceAmount() : 0);
            product.setStock(inlineProduct.getStock() != null ? inlineProduct.getStock() : 0);
            product.setStatus("active");
            productMapper.insert(product);
            registerInlineProductImageReferences(product);
            return product.getId();
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每个团购商品必须指定 productId 或 product");
        }
    }

    // ── Featured item selection ──────────────────────────────────────

    /**
     * Select the featured (热销) item from detail items.
     * Rules: soldCount desc, sortOrder asc, id asc.
     * Returns null if the list is empty.
     */
    private GroupBuyDetailItemData selectFeaturedItem(List<GroupBuyDetailItemData> items) {
        if (items == null || items.isEmpty()) return null;
        return items.stream()
                .min((a, b) -> {
                    // soldCount desc
                    int cmp = Long.compare(b.getSoldCount() != null ? b.getSoldCount() : 0,
                            a.getSoldCount() != null ? a.getSoldCount() : 0);
                    if (cmp != 0) return cmp;
                    // sortOrder asc
                    cmp = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE,
                            b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE);
                    if (cmp != 0) return cmp;
                    // id asc
                    return Long.compare(a.getId() != null ? a.getId() : Long.MAX_VALUE,
                            b.getId() != null ? b.getId() : Long.MAX_VALUE);
                })
                .orElse(null);
    }

    // ── Product summary ──────────────────────────────────────────────

    private ProductSummaryData toProductSummary(Product product) {
        if (product == null) return null;
        return ProductSummaryData.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .coverImageUrl(product.getCoverImageUrl())
                .detailImageUrls(contentValidationUtil.deserializeImageUrls(product.getDetailImageUrls()))
                .basePriceAmount(product.getBasePriceAmount())
                .status(product.getStatus())
                .build();
    }

    // ── DeliveryType validation ──────────────────────────────────────

    private boolean isValidDeliveryType(String deliveryType) {
        if (deliveryType == null) return false;
        return "express".equals(deliveryType)
                || "pickup".equals(deliveryType)
                || "local_delivery".equals(deliveryType);
    }
}
