package com.example.groupshop.order;

import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderPreviewRequest;
import com.example.groupshop.order.dto.OrderPreviewResponse;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link OrderService}.
 */
@Transactional
class OrderServiceTest extends ServiceTestBase {

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
    private MemberRelationMapper memberRelationMapper;

    @Autowired
    private OrderMapper orderMapper;

    private Long userId;
    private Long leaderId;
    private Long leaderUserId;
    private Long storeId;
    private Long groupBuyId;
    private Long groupBuyItemId;
    private Long addressId;

    @BeforeEach
    void setUp() {
        // Set up leader and store
        User leaderUser = new User();
        leaderUser.setNickname("测试团长");
        leaderUser.setPhone("13800009901");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);

        leaderUserId = leaderUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("测试团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);
        leaderId = leader.getId();

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // Create a group buy
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("测试团购");
        gbRequest.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("测试商品");
        inlineProduct.setBasePriceAmount(2000L);
        inlineProduct.setStock(100);
        item.setProduct(inlineProduct);
        item.setDisplayName("测试商品A");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(100);
        item.setSortOrder(1);
        gbRequest.setItems(List.of(item));

        GroupBuyResponse gbResponse = groupBuyService.createGroupBuy(leaderUser.getId(), gbRequest);
        groupBuyId = gbResponse.getGroupBuy().getId();
        groupBuyItemId = gbResponse.getItems().get(0).getId();

        // Set up buyer user
        User buyer = new User();
        buyer.setNickname("测试买家");
        buyer.setPhone("13800009902");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        userId = buyer.getId();

        // Create address for buyer
        CreateAddressRequest addrRequest = new CreateAddressRequest();
        addrRequest.setReceiverName("买家张三");
        addrRequest.setReceiverPhone("13800000001");
        addrRequest.setProvince("浙江省");
        addrRequest.setCity("杭州市");
        addrRequest.setDistrict("西湖区");
        addrRequest.setDetail("某某路 1 号");
        AddressResponse addrResponse = addressService.createAddress(userId, addrRequest);
        addressId = addrResponse.getId();
    }

    // ── Preview ─────────────────────────────────────────────────────────

    @Test
    void previewOrder_shouldCalculateAmounts() {
        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        OrderPreviewRequest.OrderItemEntry entry = new OrderPreviewRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        request.setItems(List.of(entry));

        OrderPreviewResponse response = orderService.previewOrder(userId, request);

        assertThat(response.getGroupBuyId()).isEqualTo(groupBuyId);
        assertThat(response.getTotalAmount()).isEqualTo(3980L); // 1990 * 2
        assertThat(response.getDiscountAmount()).isZero();
        assertThat(response.getPayAmount()).isEqualTo(3980L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("测试商品A");
        assertThat(response.getItems().get(0).getUnitPriceAmount()).isEqualTo(1990L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getAddress()).isNotNull();
    }

    @Test
    void previewOrder_shouldFailWhenInsufficientStock() {
        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        OrderPreviewRequest.OrderItemEntry entry = new OrderPreviewRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(999);
        request.setItems(List.of(entry));

        assertThatThrownBy(() -> orderService.previewOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldCreateWithSnapshots() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setRemark("请尽快发货");
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));

        OrderResponse response = orderService.createOrder(userId, request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getOrderNo()).isNotBlank();
        assertThat(response.getOrderStatus()).isEqualTo("pendingPay");
        assertThat(response.getPayStatus()).isEqualTo("unpaid");
        assertThat(response.getTotalAmount()).isEqualTo(1990L);
        assertThat(response.getPayAmount()).isEqualTo(1990L);
        assertThat(response.getReceiverName()).isEqualTo("买家张三");
        assertThat(response.getFullAddress()).isEqualTo("浙江省杭州市西湖区某某路 1 号");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("测试商品A");
        assertThat(response.getItems().get(0).getSkuName()).isEqualTo("");
        assertThat(response.getItems().get(0).getUnitPriceAmount()).isEqualTo(1990L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    void createOrder_shouldNotDeductStock() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));

        orderService.createOrder(userId, request);
        orderService.createOrder(userId, request);
        orderService.createOrder(userId, request);

        // Stock should not be deducted (that happens at simulate-pay)
        GroupBuyItem reloaded = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(reloaded.getGroupStock()).isEqualTo(100);
        assertThat(reloaded.getSoldCount()).isZero();
    }

    // ── List / Detail ───────────────────────────────────────────────────

    @Test
    void getMyOrders_shouldReturnOwnOrders() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        orderService.createOrder(userId, request);

        PageResponse<OrderResponse> result = orderService.getMyOrders(userId, null, 1, 20);
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getTotal()).isPositive();
    }

    @Test
    void getMyOrders_shouldFilterByStatus() {
        PageResponse<OrderResponse> result = orderService.getMyOrders(userId, "pendingPay", 1, 20);
        assertThat(result.getItems()).isEmpty(); // No orders yet
    }

    @Test
    void getMyOrder_shouldReturnDetail() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse created = orderService.createOrder(userId, request);

        OrderResponse detail = orderService.getMyOrder(userId, created.getId());
        assertThat(detail.getId()).isEqualTo(created.getId());
        assertThat(detail.getItems()).hasSize(1);
    }

    // ── Cancel ──────────────────────────────────────────────────────────

    @Test
    void cancelOrder_shouldCancelPendingPay() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse created = orderService.createOrder(userId, request);

        OrderResponse canceled = orderService.cancelOrder(userId, created.getId());
        assertThat(canceled.getOrderStatus()).isEqualTo("canceled");
    }

    @Test
    void cancelOrder_shouldFailWhenNotCancelable() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse created = orderService.createOrder(userId, request);

        // Cancel first
        orderService.cancelOrder(userId, created.getId());

        // Cancel again should fail
        assertThatThrownBy(() -> orderService.cancelOrder(userId, created.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_CANCELABLE);
    }

    // ── Simulate Pay (Batch 07) ────────────────────────────────────────

    private OrderResponse createOneOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        request.setItems(List.of(entry));
        return orderService.createOrder(userId, request);
    }

    @Test
    void simulatePay_shouldSucceed() {
        OrderResponse order = createOneOrder();

        OrderResponse paid = orderService.simulatePay(userId, order.getId());

        assertThat(paid.getPayStatus()).isEqualTo("paid");
        assertThat(paid.getOrderStatus()).isEqualTo("paid");
        assertThat(paid.getPaidAt()).isNotNull();

        // Stock deducted
        GroupBuyItem reloaded = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(reloaded.getGroupStock()).isEqualTo(98); // 100 - 2
        assertThat(reloaded.getSoldCount()).isEqualTo(2);

        // Member relation created
        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .eq(MemberRelation::getStoreId, storeId));
        assertThat(relation).isNotNull();
        assertThat(relation.getLevelName()).isEqualTo("V0");
        assertThat(relation.getTotalOrders()).isEqualTo(1);
        assertThat(relation.getTotalOrderAmount()).isEqualTo(3980L); // 1990 * 2
        assertThat(relation.getGrowthValue()).isEqualTo(3980);
    }

    @Test
    void simulatePay_shouldAccumulateMemberRelationOnSecondPay() {
        OrderResponse order1 = createOneOrder();
        orderService.simulatePay(userId, order1.getId());

        OrderResponse order2 = createOneOrder();
        orderService.simulatePay(userId, order2.getId());

        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .eq(MemberRelation::getStoreId, storeId));
        assertThat(relation.getTotalOrders()).isEqualTo(2);
        assertThat(relation.getTotalOrderAmount()).isEqualTo(7960L); // 3980 + 3980
        assertThat(relation.getGrowthValue()).isEqualTo(7960);
    }

    @Test
    void simulatePay_shouldFailWhenAlreadyPaid() {
        OrderResponse order = createOneOrder();
        orderService.simulatePay(userId, order.getId());

        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID);
    }

    @Test
    void simulatePay_shouldFailWhenNotPayable() {
        OrderResponse order = createOneOrder();
        orderService.cancelOrder(userId, order.getId());

        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_PAYABLE);
    }

    @Test
    void simulatePay_shouldFailWhenInsufficientStock() {
        // Create order first (stock is 100, quantity is 2 — creation succeeds)
        OrderResponse order = createOneOrder();

        // Reduce stock after creation
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        item.setGroupStock(1); // only 1 left, but order needs 2
        groupBuyItemMapper.updateById(item);

        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void simulatePay_shouldRollbackOnStockFailure() {
        // Create order first
        OrderResponse order = createOneOrder();

        // Drain stock after creation
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        item.setGroupStock(0);
        groupBuyItemMapper.updateById(item);

        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class);

        // Order should remain unpaid after rollback
        Order orderEntity = orderMapper.selectById(order.getId());
        assertThat(orderEntity.getPayStatus()).isEqualTo("unpaid");
        assertThat(orderEntity.getOrderStatus()).isEqualTo("pending_pay");
    }

    // ── Concurrent payment protection ─────────────────────────────────

    @Test
    void simulatePay_shouldRejectConcurrentDuplicateViaConditionalUpdate() {
        OrderResponse order = createOneOrder();

        // Simulate concurrent duplicate: another request already paid this order
        Order concurrent = orderMapper.selectById(order.getId());
        concurrent.setPayStatus("paid");
        concurrent.setOrderStatus("paid");
        concurrent.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(concurrent);

        // Our request should now fail with ORDER_ALREADY_PAID,
        // NOT silently double-pay
        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID);
    }

    // ── Complete Order (Batch 10) ────────────────────────────────────

    /**
     * Create a paid order and manually set it to shipped for complete-order tests.
     */
    private OrderResponse createShippedOrder() {
        OrderResponse order = createOneOrder();
        orderService.simulatePay(userId, order.getId());

        // Manually set to shipped via mapper (same effect as leader shipping)
        Order entity = orderMapper.selectById(order.getId());
        entity.setOrderStatus("shipped");
        entity.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(entity);

        return orderService.getMyOrder(userId, order.getId());
    }

    @Test
    void completeOrder_shouldSucceedAfterShipment() {
        OrderResponse order = createShippedOrder();

        OrderResponse completed = orderService.completeOrder(userId, order.getId());

        assertThat(completed.getOrderStatus()).isEqualTo("completed");
        assertThat(completed.getCompletedAt()).isNotNull();

        // DB record should also be updated
        Order entity = orderMapper.selectById(order.getId());
        assertThat(entity.getOrderStatus()).isEqualTo("completed");
        assertThat(entity.getCompletedAt()).isNotNull();
    }

    @Test
    void completeOrder_shouldFailWhenNotOwnOrder() {
        OrderResponse order = createShippedOrder();

        // Another user tries to complete
        assertThatThrownBy(() -> orderService.completeOrder(userId + 999, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void completeOrder_shouldFailWhenNotShipped() {
        OrderResponse order = createOneOrder();
        orderService.simulatePay(userId, order.getId());

        // Order is paid, not shipped — should fail
        assertThatThrownBy(() -> orderService.completeOrder(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_COMPLETABLE);
    }

    @Test
    void completeOrder_shouldFailWhenAlreadyCompleted() {
        OrderResponse order = createShippedOrder();

        // Complete first
        orderService.completeOrder(userId, order.getId());

        // Complete again should fail
        assertThatThrownBy(() -> orderService.completeOrder(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_COMPLETED);
    }
}
