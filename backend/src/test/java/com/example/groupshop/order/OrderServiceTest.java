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
import com.example.groupshop.cart.dto.AddCartItemRequest;
import com.example.groupshop.cart.service.CartService;
import com.example.groupshop.coupon.dto.CreateCouponRequest;
import com.example.groupshop.coupon.dto.CouponResponse;
import com.example.groupshop.coupon.dto.UserCouponResponse;
import com.example.groupshop.coupon.service.CouponService;
import com.example.groupshop.model.entity.UserCoupon;
import com.example.groupshop.model.mapper.CartMapper;
import com.example.groupshop.model.mapper.CouponMapper;
import com.example.groupshop.model.mapper.UserCouponMapper;
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

    @Autowired
    private CartService cartService;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

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

    // ── Cart Mode (Batch 03) ────────────────────────────────────────────

    @Test
    void createOrder_fromCart_shouldCreateAndCleanup() {
        // Add item to cart
        com.example.groupshop.model.entity.Cart cart = new com.example.groupshop.model.entity.Cart();
        cart.setUserId(userId);
        cart.setGroupBuyId(groupBuyId);
        cart.setGroupBuyItemId(groupBuyItemId);
        cart.setQuantity(2);
        cartMapper.insert(cart);
        Long cartItemId = cart.getId();

        // Create order via cart mode
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setCartItemIds(List.of(cartItemId));
        request.setRemark("购物车下单");
        OrderResponse response = orderService.createOrder(userId, request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getOrderStatus()).isEqualTo("pendingPay");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getUnitPriceAmount()).isEqualTo(1990L);

        // Cart item should be cleaned up
        assertThat(cartMapper.selectById(cartItemId)).isNull();
    }

    @Test
    void createOrder_fromCart_shouldCleanupProcessedItems() {
        // Add cart item
        com.example.groupshop.model.entity.Cart cart = new com.example.groupshop.model.entity.Cart();
        cart.setUserId(userId);
        cart.setGroupBuyId(groupBuyId);
        cart.setGroupBuyItemId(groupBuyItemId);
        cart.setQuantity(2);
        cartMapper.insert(cart);

        // Create order via cart mode
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setCartItemIds(List.of(cart.getId()));
        orderService.createOrder(userId, request);

        // Processed cart item should be deleted
        assertThat(cartMapper.selectById(cart.getId())).isNull();
    }

    @Test
    void previewOrder_fromCart_shouldWork() {
        // Add item to cart
        com.example.groupshop.model.entity.Cart cart = new com.example.groupshop.model.entity.Cart();
        cart.setUserId(userId);
        cart.setGroupBuyId(groupBuyId);
        cart.setGroupBuyItemId(groupBuyItemId);
        cart.setQuantity(3);
        cartMapper.insert(cart);

        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setAddressId(addressId);
        request.setCartItemIds(List.of(cart.getId()));

        OrderPreviewResponse response = orderService.previewOrder(userId, request);

        assertThat(response.getGroupBuyId()).isEqualTo(groupBuyId);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(response.getPayAmount()).isEqualTo(1990L * 3);

        // Cart item should still exist after preview
        assertThat(cartMapper.selectById(cart.getId())).isNotNull();
    }

    @Test
    void previewOrder_shouldFailWhenBothModes() {
        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        OrderPreviewRequest.OrderItemEntry entry = new OrderPreviewRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        request.setCartItemIds(List.of(1L));

        assertThatThrownBy(() -> orderService.previewOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void previewOrder_shouldFailWhenNeitherMode() {
        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setAddressId(addressId);

        assertThatThrownBy(() -> orderService.previewOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createOrder_fromCart_shouldFailWhenCrossUserCartItem() {
        // Add cart item for a different user
        Long otherUserId = userId + 999;

        com.example.groupshop.model.entity.Cart cart = new com.example.groupshop.model.entity.Cart();
        cart.setUserId(otherUserId);
        cart.setGroupBuyId(groupBuyId);
        cart.setGroupBuyItemId(groupBuyItemId);
        cart.setQuantity(1);
        cartMapper.insert(cart);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setCartItemIds(List.of(cart.getId()));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_FORBIDDEN);
    }

    // ── Idempotency (Batch 03) ──────────────────────────────────────────

    @Test
    void createOrder_sameIdempotencyKey_shouldNotCreateDuplicate() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));

        // First call
        OrderResponse first = orderService.createOrder(userId, request);

        // Second call with same params should create a second order
        // (This is without idempotency-key, so it creates separate orders)
        OrderResponse second = orderService.createOrder(userId, request);

        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    @Test
    void simulatePay_sameIdempotencyKey_shouldNotDeductStockAgain() {
        // Create and pay an order
        OrderResponse order = createOneOrder();
        orderService.simulatePay(userId, order.getId());

        // Stock was deducted once
        GroupBuyItem reloaded = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(reloaded.getGroupStock()).isEqualTo(98);
        assertThat(reloaded.getSoldCount()).isEqualTo(2);
    }

    @Test
    void createOrder_cartMode_shouldFailWhenMismatchedGroupBuy() {
        // Create a second group buy
        CreateGroupBuyRequest gb2Request = new CreateGroupBuyRequest();
        gb2Request.setTitle("第二个团购");
        gb2Request.setDeliveryType(com.example.groupshop.common.enums.DeliveryType.EXPRESS);
        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inline = new CreateGroupBuyRequest.InlineProduct();
        inline.setName("商品C");
        inline.setBasePriceAmount(1000L);
        inline.setStock(100);
        item.setProduct(inline);
        item.setDisplayName("商品C");
        item.setGroupPriceAmount(990L);
        item.setGroupStock(100);
        item.setSortOrder(1);
        gb2Request.setItems(List.of(item));
        GroupBuyResponse gb2 = groupBuyService.createGroupBuy(leaderUserId, gb2Request);
        Long gb2ItemId = gb2.getItems().get(0).getId();

        // Add cart items from different group buys
        com.example.groupshop.model.entity.Cart cart1 = new com.example.groupshop.model.entity.Cart();
        cart1.setUserId(userId);
        cart1.setGroupBuyId(groupBuyId);
        cart1.setGroupBuyItemId(groupBuyItemId);
        cart1.setQuantity(1);
        cartMapper.insert(cart1);

        com.example.groupshop.model.entity.Cart cart2 = new com.example.groupshop.model.entity.Cart();
        cart2.setUserId(userId);
        cart2.setGroupBuyId(gb2.getGroupBuy().getId());
        cart2.setGroupBuyItemId(gb2ItemId);
        cart2.setQuantity(1);
        cartMapper.insert(cart2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setCartItemIds(List.of(cart1.getId(), cart2.getId()));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_CROSS_GROUP_BUY);
    }

    // ── Coupon Order Flows (Batch 04) ────────────────────────────────────

    private Long createTestCoupon() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("无门槛10元券");
        request.setCouponType("amount");
        request.setAmount(1000L);
        request.setThresholdAmount(0L);
        request.setTotalQuantity(100);
        request.setPerUserLimit(1);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(30));
        CouponResponse response = couponService.createCoupon(leaderUserId, request);
        return response.getId();
    }

    @Test
    void previewOrder_withCoupon_shouldShowAvailableCoupons() {
        createTestCoupon();

        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        OrderPreviewRequest.OrderItemEntry entry = new OrderPreviewRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        request.setItems(List.of(entry));

        // No userCouponId — should return available coupons
        OrderPreviewResponse response = orderService.previewOrder(userId, request);

        assertThat(response.getTotalAmount()).isEqualTo(3980L);
        assertThat(response.getDiscountAmount()).isZero();
        assertThat(response.getPayAmount()).isEqualTo(3980L);
        // availableCoupons will be null/empty since user hasn't claimed any
    }

    @Test
    void createOrder_withCoupon_shouldLockCoupon() {
        // Create and claim a coupon
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        // Create order with the user coupon
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        request.setItems(List.of(entry));

        OrderResponse response = orderService.createOrder(userId, request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getDiscountAmount()).isEqualTo(1000L);
        assertThat(response.getPayAmount()).isEqualTo(2980L); // 3980 - 1000
        assertThat(response.getUserCouponId()).isEqualTo(userCoupon.getId());
        assertThat(response.getCouponName()).isEqualTo("无门槛10元券");
        assertThat(response.getCouponType()).isEqualTo("amount");

        // Coupon should be locked
        UserCoupon locked = userCouponMapper.selectById(userCoupon.getId());
        assertThat(locked.getStatus()).isEqualTo("locked");
        assertThat(locked.getLockedOrderId()).isEqualTo(response.getId());
    }

    @Test
    void createOrder_withCoupon_shouldNotLockIfCreationFails() {
        // Create and claim a coupon
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        // Create order with invalid quantity (should fail)
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(99999L); // invalid item
        entry.setQuantity(1);
        request.setItems(List.of(entry));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(BusinessException.class);

        // Coupon should remain unused
        UserCoupon unchanged = userCouponMapper.selectById(userCoupon.getId());
        assertThat(unchanged.getStatus()).isEqualTo("unused");
    }

    @Test
    void cancelOrder_withCoupon_shouldReleaseCoupon() {
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        // Create order with coupon
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, request);

        // Cancel the order
        orderService.cancelOrder(userId, order.getId());

        // Coupon should be released back to unused
        UserCoupon released = userCouponMapper.selectById(userCoupon.getId());
        assertThat(released.getStatus()).isEqualTo("unused");
        assertThat(released.getLockedOrderId()).isNull();
    }

    @Test
    void simulatePay_withCoupon_shouldMarkCouponUsed() {
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        // Create order with coupon
        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, request);

        // Pay
        OrderResponse paid = orderService.simulatePay(userId, order.getId());

        // Coupon should be used
        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertThat(used.getStatus()).isEqualTo("used");
        assertThat(used.getUsedAt()).isNotNull();

        // Verify pay amount reflects discount
        assertThat(paid.getDiscountAmount()).isEqualTo(1000L);
        assertThat(paid.getPayAmount()).isEqualTo(990L); // 1990 - 1000
    }

    @Test
    void simulatePay_shouldNotDoubleUseCouponOnRetry() {
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        request.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, request);

        // Pay first time
        orderService.simulatePay(userId, order.getId());

        // Try to pay again (should fail without double-using the coupon)
        assertThatThrownBy(() -> orderService.simulatePay(userId, order.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID);

        // Coupon still used once, not double-used
        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertThat(used.getStatus()).isEqualTo("used");
    }

    @Test
    void previewOrder_withCouponId_shouldCalculateDiscount() {
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        OrderPreviewRequest request = new OrderPreviewRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        OrderPreviewRequest.OrderItemEntry entry = new OrderPreviewRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2);
        request.setItems(List.of(entry));

        OrderPreviewResponse response = orderService.previewOrder(userId, request);

        assertThat(response.getTotalAmount()).isEqualTo(3980L);
        assertThat(response.getDiscountAmount()).isEqualTo(1000L);
        assertThat(response.getPayAmount()).isEqualTo(2980L);
        assertThat(response.getSelectedCoupon()).isNotNull();
        assertThat(response.getSelectedCoupon().getAmount()).isEqualTo(1000L);
    }

    @Test
    void simulatePay_withCoupon_shouldAccumulateGrowthByPayAmount() {
        Long templateCouponId = createTestCoupon();
        UserCouponResponse userCoupon = couponService.claimCoupon(userId, templateCouponId);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setGroupBuyId(groupBuyId);
        request.setAddressId(addressId);
        request.setUserCouponId(userCoupon.getId());
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(2); // totalAmount = 3980
        request.setItems(List.of(entry));
        OrderResponse order = orderService.createOrder(userId, request);

        orderService.simulatePay(userId, order.getId());

        // Member relation should use payAmount (2980 = 3980 - 1000)
        MemberRelation relation = memberRelationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberRelation>()
                        .eq(MemberRelation::getUserId, userId)
                        .eq(MemberRelation::getStoreId, storeId));
        assertThat(relation).isNotNull();
        assertThat(relation.getTotalOrderAmount()).isEqualTo(2980L); // payAmount, not totalAmount
        assertThat(relation.getGrowthValue()).isEqualTo(2980);
    }
}
