package com.example.groupshop.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.groupshop.browsing.service.BrowsingHistoryService;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.favorite.service.FavoriteService;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest.InlineProduct;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest.ItemEntry;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse.GroupBuyData;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse.GroupBuyItemData;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.UpdateGroupBuyRequest.UpdateItemEntry;
import com.example.groupshop.model.entity.GroupBuy;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.OrderItem;
import com.example.groupshop.model.entity.Product;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.GroupBuyMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        validateEndTimeAfterStart(startTime, endTime);

        GroupBuy groupBuy = new GroupBuy();
        groupBuy.setStoreId(store.getId());
        groupBuy.setLeaderId(leader.getId());
        groupBuy.setTitle(request.getTitle());
        groupBuy.setIntroduction(request.getIntroduction());
        groupBuy.setCoverImageUrl(request.getCoverImageUrl());
        groupBuy.setGroupType("normal");
        groupBuy.setDeliveryType(request.getDeliveryType().getValue());
        groupBuy.setShippingTime(parseIsoDateTime(request.getShippingTime()));
        groupBuy.setStartTime(startTime);
        groupBuy.setEndTime(endTime);
        groupBuy.setVisibility("public");
        groupBuy.setStatus("published");
        groupBuyMapper.insert(groupBuy);

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

        return GroupBuyResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .items(itemResponses)
                .build();
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
        validateEndTimeAfterStart(groupBuy.getStartTime(), groupBuy.getEndTime());
        groupBuyMapper.updateById(groupBuy);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
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

    // ── Public browsing ───────────────────────────────────────────────

    /**
     * List public published group buys with optional keyword and categoryId filtering.
     * Public lists do NOT return visibility.
     */
    public PageResponse<PublicGroupBuyItem> getPublicGroupBuys(int page, int pageSize,
                                                                String keyword, Long categoryId) {
        Page<GroupBuy> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<GroupBuy> wrapper = new LambdaQueryWrapper<GroupBuy>()
                .eq(GroupBuy::getStatus, "published")
                .eq(GroupBuy::getVisibility, "public")
                .orderByDesc(GroupBuy::getCreatedAt);

        // Validate status parameter is not passed (should be rejected at controller level)
        // Keyword filtering
        if (keyword != null && !keyword.isBlank()) {
            Set<Long> keywordMatchIds = new HashSet<>();

            // Match by title or introduction
            List<GroupBuy> titleMatches = groupBuyMapper.selectList(
                    new LambdaQueryWrapper<GroupBuy>()
                            .like(GroupBuy::getTitle, keyword)
                            .or().like(GroupBuy::getIntroduction, keyword)
                            .eq(GroupBuy::getStatus, "published")
                            .eq(GroupBuy::getVisibility, "public"));
            titleMatches.forEach(gb -> keywordMatchIds.add(gb.getId()));

            // Match by item displayName
            List<GroupBuyItem> itemMatches = groupBuyItemMapper.selectList(
                    new LambdaQueryWrapper<GroupBuyItem>()
                            .like(GroupBuyItem::getDisplayName, keyword));
            itemMatches.forEach(item -> keywordMatchIds.add(item.getGroupBuyId()));

            // Match by product name
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

        Page<GroupBuy> result = groupBuyMapper.selectPage(pageObj, wrapper);

        List<PublicGroupBuyItem> items = result.getRecords().stream()
                .map(this::toPublicGroupBuyItem)
                .collect(Collectors.toList());

        return PageResponse.of(items, page, pageSize, result.getTotal());
    }

    /**
     * Get public group buy detail.
     * Returns visibility in groupBuy.visibility (per MVP contract).
     * Shows viewer.favorited for authenticated users.
     * Records browsing history for authenticated users (auxiliary, failure logged only).
     */
    public GroupBuyDetailResponse getPublicGroupBuyDetail(Long groupBuyId, Long viewerUserId) {
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

        StoreDetail storeDetail = StoreDetail.builder()
                .id(store.getId())
                .name(store.getName())
                .logoUrl(store.getLogoUrl())
                .build();

        boolean subscribed = viewerUserId != null
                && subscriptionService.isSubscribed(viewerUserId, groupBuy.getLeaderId());

        boolean favorited = favoriteService.isFavorited(viewerUserId, groupBuyId);

        return GroupBuyDetailResponse.builder()
                .groupBuy(toGroupBuyData(groupBuy))
                .leader(leaderDetail)
                .store(storeDetail)
                .items(items.stream().map(this::toGroupBuyDetailItemData).collect(Collectors.toList()))
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
            product.setBasePriceAmount(inlineProduct.getBasePriceAmount());
            product.setStock(inlineProduct.getStock());
            product.setStatus("active");
            productMapper.insert(product);
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
        return GroupBuyDetailItemData.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .displayName(item.getDisplayName())
                .groupPriceAmount(item.getGroupPriceAmount())
                .groupStock(item.getGroupStock())
                .soldCount(item.getSoldCount())
                .sortOrder(item.getSortOrder())
                .coverImageUrl(product != null ? product.getCoverImageUrl() : null)
                .build();
    }

    // ── Converters ────────────────────────────────────────────────────

    public PublicGroupBuyItem toPublicGroupBuyItem(GroupBuy gb) {
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

        StoreLite storeLite = StoreLite.builder()
                .id(store.getId())
                .name(store.getName())
                .build();

        return PublicGroupBuyItem.builder()
                .id(gb.getId())
                .title(gb.getTitle())
                .coverImageUrl(gb.getCoverImageUrl())
                .status(gb.getStatus())
                .endTime(gb.getEndTime() != null ? gb.getEndTime().toString() : null)
                .minPriceAmount(minPriceAmount)
                .soldCount(soldCount)
                .leader(leaderLite)
                .store(storeLite)
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
}
