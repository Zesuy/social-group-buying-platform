package com.example.groupshop.aftersale;

import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.aftersale.dto.AfterSaleResponse;
import com.example.groupshop.aftersale.dto.CreateAfterSaleRequest;
import com.example.groupshop.aftersale.dto.RejectAfterSaleRequest;
import com.example.groupshop.aftersale.service.AfterSaleService;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.AfterSale;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.AfterSaleMapper;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AfterSaleService}.
 *
 * <p>Covers state transitions, order status updates, stock restoration,
 * member stats reversal, and idempotent refund.
 */
@Transactional
class AfterSaleServiceTest extends ServiceTestBase {

    @Autowired
    private AfterSaleService afterSaleService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private GroupBuyItemMapper groupBuyItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired
    private MemberRelationMapper memberRelationMapper;

    private Long userId;
    private Long leaderUserId;
    private Long storeId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long addressId;

    @BeforeEach
    void setUp() {
        // ── Create leader + store ──────────────────────────────────────────
        User leaderUser = new User();
        leaderUser.setNickname("售后团长");
        leaderUser.setPhone("13800009910");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("售后团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("售后测试店铺");
        store.setDefaultDeliveryType("express");
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // ── Create group buy ──────────────────────────────────────────────
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("售后测试团购");
        gbRequest.setDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline = new CreateGroupBuyRequest.InlineProduct();
        inline.setName("售后测试商品");
        inline.setBasePriceAmount(2000L);
        inline.setStock(100);
        item.setProduct(inline);
        item.setDisplayName("售后测试商品");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(100);
        item.setSortOrder(1);
        gbRequest.setItems(List.of(item));
        GroupBuyResponse gbResponse = groupBuyService.createGroupBuy(leaderUserId, gbRequest);
        groupBuyId = gbResponse.getGroupBuy().getId();
        groupBuyItemId = gbResponse.getItems().get(0).getId();

        // ── Create buyer ──────────────────────────────────────────────────
        User buyer = new User();
        buyer.setNickname("售后买家");
        buyer.setPhone("13800009911");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        userId = buyer.getId();

        // ── Create address ────────────────────────────────────────────────
        CreateAddressRequest addrReq = new CreateAddressRequest();
        addrReq.setReceiverName("买家张三");
        addrReq.setReceiverPhone("13800000001");
        addrReq.setProvince("浙江省");
        addrReq.setCity("杭州市");
        addrReq.setDistrict("西湖区");
        addrReq.setDetail("某某路 1 号");
        addressId = addressService.createAddress(userId, addrReq).getId();
    }

    // ── Helper: create a paid order ───────────────────────────────────────

    private Long createPaidOrder() {
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setGroupBuyId(groupBuyId);
        orderReq.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        orderReq.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, orderReq);
        orderService.simulatePay(userId, order.getId());

        // Verify stock deducted
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(item.getGroupStock()).isEqualTo(98);
        assertThat(item.getSoldCount()).isEqualTo(2);

        // Verify member relation created
        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .eq(MemberRelation::getStoreId, storeId));
        assertThat(relation).isNotNull();
        assertThat(relation.getTotalOrderAmount()).isEqualTo(3980L); // 1990 * 2
        assertThat(relation.getGrowthValue()).isEqualTo(3980);
        assertThat(relation.getTotalOrders()).isEqualTo(1);

        return order.getId();
    }

    // ── 1. Create After-Sale ──────────────────────────────────────────────

    @Test
    void createAfterSale_shouldSucceed() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");

        AfterSaleResponse response = afterSaleService.createAfterSale(userId, orderId, request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getType()).isEqualTo("refund");
        assertThat(response.getReason()).isEqualTo("商品质量问题");
        assertThat(response.getAmount()).isEqualTo(3980L);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getCreatedAt()).isNotNull();

        // Order should be in after_sale status
        Order order = orderMapper.selectById(orderId);
        assertThat(order.getOrderStatus()).isEqualTo("after_sale");
    }

    @Test
    void createAfterSale_shouldFailWhenNotApplicable() {
        // Create order but don't pay it
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setGroupBuyId(groupBuyId);
        orderReq.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        orderReq.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, orderReq);

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("不想要了");

        // Unpaid order cannot apply for after-sale
        assertThatThrownBy(() -> afterSaleService.createAfterSale(userId, order.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AFTER_SALE_NOT_APPLICABLE);
    }

    @Test
    void createAfterSale_shouldFailWhenInProgress() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");

        // First application succeeds
        afterSaleService.createAfterSale(userId, orderId, request);

        // Second application should fail
        assertThatThrownBy(() -> afterSaleService.createAfterSale(userId, orderId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AFTER_SALE_IN_PROGRESS);
    }

    @Test
    void createAfterSale_shouldFailWhenNotOwnOrder() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");

        // Another user tries to apply
        Long otherUserId = userId + 999;
        assertThatThrownBy(() -> afterSaleService.createAfterSale(otherUserId, orderId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ── 2. List / Detail (Buyer) ──────────────────────────────────────────

    @Test
    void getMyAfterSales_shouldReturnOwnRecords() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        afterSaleService.createAfterSale(userId, orderId, request);

        PageResponse<AfterSaleResponse> result = afterSaleService.getMyAfterSales(userId, 1, 20);
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getTotal()).isPositive();
        assertThat(result.getItems().get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void getMyAfterSale_shouldReturnDetail() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        AfterSaleResponse detail = afterSaleService.getMyAfterSale(userId, created.getId());
        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getReason()).isEqualTo("质量问题");
        assertThat(detail.getOrderNo()).isNotBlank();
    }

    // ── 3. Store After-Sales ──────────────────────────────────────────────

    @Test
    void getStoreAfterSales_shouldReturnStoreRecords() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        afterSaleService.createAfterSale(userId, orderId, request);

        PageResponse<AfterSaleResponse> result = afterSaleService.getStoreAfterSales(leaderUserId, 1, 20);
        assertThat(result.getItems()).isNotEmpty();
    }

    @Test
    void getStoreAfterSales_shouldFilterByStatus() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);
        afterSaleService.approveAfterSale(leaderUserId, created.getId());

        PageResponse<AfterSaleResponse> approved = afterSaleService.getStoreAfterSales(leaderUserId, "approved", 1, 20);
        PageResponse<AfterSaleResponse> pending = afterSaleService.getStoreAfterSales(leaderUserId, "pending", 1, 20);

        assertThat(approved.getItems()).extracting(AfterSaleResponse::getId).contains(created.getId());
        assertThat(pending.getItems()).extracting(AfterSaleResponse::getId).doesNotContain(created.getId());
    }

    @Test
    void getStoreAfterSale_shouldReturnDetail() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        AfterSaleResponse detail = afterSaleService.getStoreAfterSale(leaderUserId, created.getId());
        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getStoreId()).isEqualTo(storeId);
    }

    // ── 4. Approve After-Sale ─────────────────────────────────────────────

    @Test
    void approveAfterSale_shouldSucceed() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        AfterSaleResponse approved = afterSaleService.approveAfterSale(leaderUserId, created.getId());

        assertThat(approved.getStatus()).isEqualTo("approved");
        assertThat(approved.getApprovedAt()).isNotNull();

        // Order stays in after_sale
        Order order = orderMapper.selectById(orderId);
        assertThat(order.getOrderStatus()).isEqualTo("after_sale");
    }

    @Test
    void approveAfterSale_shouldFailWhenNotPending() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        // Approve first
        afterSaleService.approveAfterSale(leaderUserId, created.getId());

        // Approve again should fail
        assertThatThrownBy(() -> afterSaleService.approveAfterSale(leaderUserId, created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AFTER_SALE_NOT_APPROVABLE);
    }

    @Test
    void approveAfterSale_shouldFailForCrossStore() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        // A different user with a different store tries to approve
        User otherLeaderUser = new User();
        otherLeaderUser.setNickname("其他团长");
        otherLeaderUser.setPhone("13800009912");
        otherLeaderUser.setStatus("normal");
        userMapper.insert(otherLeaderUser);

        Leader otherLeader = new Leader();
        otherLeader.setUserId(otherLeaderUser.getId());
        otherLeader.setDisplayName("其他团长");
        otherLeader.setServiceStatus("normal");
        otherLeader.setMemberCount(0);
        otherLeader.setFollowerCount(0);
        leaderMapper.insert(otherLeader);

        Store otherStore = new Store();
        otherStore.setLeaderId(otherLeader.getId());
        otherStore.setName("其他店铺");
        otherStore.setDefaultDeliveryType("express");
        otherStore.setStatus("active");
        storeMapper.insert(otherStore);

        assertThatThrownBy(() -> afterSaleService.approveAfterSale(otherLeaderUser.getId(), created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    // ── 5. Reject After-Sale ──────────────────────────────────────────────

    @Test
    void rejectAfterSale_shouldSucceed() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        RejectAfterSaleRequest rejectReq = new RejectAfterSaleRequest();
        rejectReq.setRejectReason("商品完好，不予退款");

        AfterSaleResponse rejected = afterSaleService.rejectAfterSale(leaderUserId, created.getId(), rejectReq);

        assertThat(rejected.getStatus()).isEqualTo("rejected");
        assertThat(rejected.getRejectReason()).isEqualTo("商品完好，不予退款");
        assertThat(rejected.getRejectedAt()).isNotNull();

        // Order should be restored to original status
        Order order = orderMapper.selectById(orderId);
        assertThat(order.getOrderStatus()).isEqualTo("paid");
    }

    @Test
    void rejectAfterSale_shouldFailWhenNotPending() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        // Reject first
        RejectAfterSaleRequest rejectReq = new RejectAfterSaleRequest();
        rejectReq.setRejectReason("拒绝");
        afterSaleService.rejectAfterSale(leaderUserId, created.getId(), rejectReq);

        // Reject again should fail
        RejectAfterSaleRequest rejectReq2 = new RejectAfterSaleRequest();
        rejectReq2.setRejectReason("再次拒绝");
        assertThatThrownBy(() -> afterSaleService.rejectAfterSale(leaderUserId, created.getId(), rejectReq2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AFTER_SALE_NOT_REJECTABLE);
    }

    // ── 6. Complete Refund ────────────────────────────────────────────────

    @Test
    void completeRefund_shouldRestoreStockAndReverseMemberStats() {
        Long orderId = createPaidOrder();

        // Create and approve after-sale
        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);
        afterSaleService.approveAfterSale(leaderUserId, created.getId());

        // Complete refund
        AfterSaleResponse completed = afterSaleService.completeRefund(leaderUserId, created.getId());

        assertThat(completed.getStatus()).isEqualTo("completed");
        assertThat(completed.getCompletedAt()).isNotNull();

        // Order should be refunded
        Order order = orderMapper.selectById(orderId);
        assertThat(order.getOrderStatus()).isEqualTo("refunded");
        assertThat(order.getPayStatus()).isEqualTo("refunded");

        // Stock should be restored (100 - 2 + 2 = 100, soldCount 2 - 2 = 0)
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(item.getGroupStock()).isEqualTo(100);
        assertThat(item.getSoldCount()).isEqualTo(0);

        // Member stats should be reversed
        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .eq(MemberRelation::getStoreId, storeId));
        assertThat(relation).isNotNull();
        assertThat(relation.getTotalOrderAmount()).isZero();
        assertThat(relation.getGrowthValue()).isZero();
        assertThat(relation.getTotalOrders()).isZero();
    }

    @Test
    void completeRefund_shouldBeIdempotent() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);
        afterSaleService.approveAfterSale(leaderUserId, created.getId());

        // First refund
        AfterSaleResponse first = afterSaleService.completeRefund(leaderUserId, created.getId());
        assertThat(first.getStatus()).isEqualTo("completed");

        // Second refund - should return current state without side effects
        AfterSaleResponse second = afterSaleService.completeRefund(leaderUserId, created.getId());
        assertThat(second.getStatus()).isEqualTo("completed");

        // Stock should not be restored twice
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(item.getGroupStock()).isEqualTo(100);
        assertThat(item.getSoldCount()).isEqualTo(0);

        // Verify only one after_sale record exists
        long count = afterSaleMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AfterSale>()
                        .eq(AfterSale::getOrderId, orderId));
        assertThat(count).isEqualTo(1);
    }

    @Test
    void completeRefund_shouldFailWhenNotApproved() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("质量问题");
        AfterSaleResponse created = afterSaleService.createAfterSale(userId, orderId, request);

        // Try to complete refund without approving first
        assertThatThrownBy(() -> afterSaleService.completeRefund(leaderUserId, created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AFTER_SALE_NOT_REFUNDABLE);
    }

    // ── 7. Order Response: afterSale Summary ──────────────────────────────

    @Test
    void getMyOrder_shouldIncludeAfterSaleSummary() {
        Long orderId = createPaidOrder();

        // No after-sale yet — afterSale should be null
        OrderResponse noAfterSale = orderService.getMyOrder(userId, orderId);
        assertThat(noAfterSale.getAfterSale()).isNull();

        // Create after-sale
        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        afterSaleService.createAfterSale(userId, orderId, request);

        // Order should now include afterSale summary
        OrderResponse withAfterSale = orderService.getMyOrder(userId, orderId);
        assertThat(withAfterSale.getAfterSale()).isNotNull();
        assertThat(withAfterSale.getAfterSale().getStatus()).isEqualTo("pending");
        assertThat(withAfterSale.getAfterSale().getType()).isEqualTo("refund");
        assertThat(withAfterSale.getAfterSale().getReason()).isEqualTo("商品质量问题");
        assertThat(withAfterSale.getAfterSale().getAmount()).isEqualTo(3980L);
    }

    @Test
    void getStoreOrder_shouldIncludeAfterSaleSummary() {
        Long orderId = createPaidOrder();

        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setType("refund");
        request.setReason("商品质量问题");
        afterSaleService.createAfterSale(userId, orderId, request);

        // Store order detail should also include afterSale summary
        OrderResponse storeOrder = orderService.getMyOrder(userId, orderId);
        assertThat(storeOrder.getAfterSale()).isNotNull();
        assertThat(storeOrder.getAfterSale().getStatus()).isEqualTo("pending");
    }
}
