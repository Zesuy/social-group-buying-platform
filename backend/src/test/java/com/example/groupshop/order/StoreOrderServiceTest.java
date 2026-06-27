package com.example.groupshop.order;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.response.PageResponse;
import com.example.groupshop.groupbuy.dto.CreateGroupBuyRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyResponse;
import com.example.groupshop.groupbuy.service.GroupBuyService;
import com.example.groupshop.model.entity.GroupBuyItem;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.MemberRelation;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.model.entity.Shipment;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.mapper.GroupBuyItemMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.MemberRelationMapper;
import com.example.groupshop.model.mapper.OrderMapper;
import com.example.groupshop.model.mapper.ShipmentMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserMapper;
import com.example.groupshop.order.dto.CreateOrderRequest;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.dto.ShipOrderRequest;
import com.example.groupshop.order.dto.ShipOrderResponse;
import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import com.example.groupshop.order.service.OrderService;
import com.example.groupshop.order.service.StoreOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link StoreOrderService}.
 */
@Transactional
class StoreOrderServiceTest extends ServiceTestBase {

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GroupBuyService groupBuyService;

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
    private ShipmentMapper shipmentMapper;

    @Autowired
    private MemberRelationMapper memberRelationMapper;

    @Autowired
    private AddressService addressService;

    private Long leaderUserId;
    private Long buyerUserId;
    private Long storeId;
    private Long groupBuyItemId;
    private Long addressId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        // Set up leader and store
        User leaderUser = new User();
        leaderUser.setNickname("团长");
        leaderUser.setPhone("13800009903");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("团长");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("团长店铺");
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
        item.setDisplayName("测试商品");
        item.setGroupPriceAmount(1990L);
        item.setGroupStock(50);
        item.setSortOrder(1);
        gbRequest.setItems(List.of(item));

        GroupBuyResponse gbResponse = groupBuyService.createGroupBuy(leaderUser.getId(), gbRequest);
        groupBuyItemId = gbResponse.getItems().get(0).getId();

        // Set up buyer
        User buyer = new User();
        buyer.setNickname("买家");
        buyer.setPhone("13800009904");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        buyerUserId = buyer.getId();

        // Create an address for the buyer
        CreateAddressRequest addrReq = new CreateAddressRequest();
        addrReq.setReceiverName("买家李四");
        addrReq.setReceiverPhone("13800000002");
        addrReq.setProvince("广东省");
        addrReq.setCity("深圳市");
        addrReq.setDistrict("南山区");
        addrReq.setDetail("科技园路 100 号");
        AddressResponse addrResp = addressService.createAddress(buyerUserId, addrReq);
        addressId = addrResp.getId();

        // Create and pay an order
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setGroupBuyId(gbResponse.getGroupBuy().getId());
        orderReq.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry orderItem = new CreateOrderRequest.OrderItemEntry();
        orderItem.setGroupBuyItemId(groupBuyItemId);
        orderItem.setQuantity(2);
        orderReq.setItems(List.of(orderItem));
        OrderResponse created = orderService.createOrder(buyerUserId, orderReq);
        orderId = created.getId();

        // Pay the order
        orderService.simulatePay(buyerUserId, orderId);
    }

    // ── List Store Orders ───────────────────────────────────────────────

    @Test
    void getStoreOrders_shouldReturnOrders() {
        PageResponse<OrderResponse> result = storeOrderService.getStoreOrders(leaderUserId, null, 1, 20);
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getTotal()).isPositive();
    }

    @Test
    void getStoreOrders_shouldFilterByStatus() {
        PageResponse<OrderResponse> result = storeOrderService.getStoreOrders(leaderUserId, "paid", 1, 20);
        assertThat(result.getItems()).isNotEmpty();
    }

    @Test
    void getStoreOrders_shouldFailForNonLeader() {
        assertThatThrownBy(() -> storeOrderService.getStoreOrders(buyerUserId, null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LEADER_REQUIRED);
    }

    // ── Get Store Order Detail ──────────────────────────────────────────

    @Test
    void getStoreOrder_shouldReturnDetail() {
        OrderResponse detail = storeOrderService.getStoreOrder(leaderUserId, orderId);
        assertThat(detail.getId()).isEqualTo(orderId);
        assertThat(detail.getPayStatus()).isEqualTo("paid");
    }

    @Test
    void getStoreOrder_shouldFailForNonLeader() {
        assertThatThrownBy(() -> storeOrderService.getStoreOrder(buyerUserId, orderId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LEADER_REQUIRED);
    }

    @Test
    void getStoreOrder_shouldFailForStoreForbidden() {
        // Create another leader and store
        User otherLeader = new User();
        otherLeader.setNickname("其他团长");
        otherLeader.setPhone("13800009905");
        otherLeader.setStatus("normal");
        userMapper.insert(otherLeader);

        Leader otherL = new Leader();
        otherL.setUserId(otherLeader.getId());
        otherL.setDisplayName("其他团长");
        otherL.setServiceStatus("normal");
        otherL.setMemberCount(0);
        otherL.setFollowerCount(0);
        leaderMapper.insert(otherL);

        Store otherStore = new Store();
        otherStore.setLeaderId(otherL.getId());
        otherStore.setName("其他店铺");
        otherStore.setDefaultDeliveryType("express");
        otherStore.setDistributionEnabled(false);
        otherStore.setStatus("active");
        storeMapper.insert(otherStore);

        assertThatThrownBy(() -> storeOrderService.getStoreOrder(otherLeader.getId(), orderId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    // ── Ship Order ──────────────────────────────────────────────────────

    @Test
    void shipOrder_shouldSucceed() {
        ShipOrderRequest request = ShipOrderRequest.builder()
                .deliveryType(DeliveryType.EXPRESS)
                .logisticsCompany("顺丰速运")
                .trackingNo("SF1234567890")
                .build();

        ShipOrderResponse response = storeOrderService.shipOrder(leaderUserId, orderId, request);

        assertThat(response.getOrder().getOrderStatus()).isEqualTo("shipped");
        assertThat(response.getOrder().getShippedAt()).isNotNull();
        assertThat(response.getShipment()).isNotNull();
        assertThat(response.getShipment().getDeliveryType()).isEqualTo("express");
        assertThat(response.getShipment().getLogisticsCompany()).isEqualTo("顺丰速运");
        assertThat(response.getShipment().getTrackingNo()).isEqualTo("SF1234567890");

        // Verify shipment record
        Shipment shipment = shipmentMapper.selectById(response.getShipment().getId());
        assertThat(shipment).isNotNull();
        assertThat(shipment.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void shipOrder_shouldFailWhenAlreadyShipped() {
        ShipOrderRequest request = ShipOrderRequest.builder()
                .deliveryType(DeliveryType.EXPRESS)
                .build();

        storeOrderService.shipOrder(leaderUserId, orderId, request);

        assertThatThrownBy(() -> storeOrderService.shipOrder(leaderUserId, orderId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_SHIPPED);
    }

    @Test
    void shipOrder_shouldFailWhenOrderNotPaid() {
        // Create a new unpaid order
        CreateOrderRequest orderReq = new CreateOrderRequest();
        orderReq.setGroupBuyId(groupBuyId());
        orderReq.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry orderItem = new CreateOrderRequest.OrderItemEntry();
        orderItem.setGroupBuyItemId(groupBuyItemId);
        orderItem.setQuantity(1);
        orderReq.setItems(List.of(orderItem));
        OrderResponse unpaidOrder = orderService.createOrder(buyerUserId, orderReq);

        assertThatThrownBy(() -> storeOrderService.shipOrder(leaderUserId, unpaidOrder.getId(),
                ShipOrderRequest.builder().deliveryType(DeliveryType.EXPRESS).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_SHIPPABLE);
    }

    // ── Concurrent shipment protection ────────────────────────────────

    @Test
    void shipOrder_shouldRejectConcurrentDuplicateViaConditionalUpdate() {
        // Simulate concurrent duplicate: another request already shipped this order
        Order current = orderMapper.selectById(orderId);
        current.setOrderStatus("shipped");
        current.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(current);

        assertThatThrownBy(() -> storeOrderService.shipOrder(leaderUserId, orderId,
                ShipOrderRequest.builder().deliveryType(DeliveryType.EXPRESS).build()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ALREADY_SHIPPED);
    }

    private Long groupBuyId() {
        // Get the group buy ID from the order
        Order order = orderMapper.selectById(orderId);
        return order != null ? order.getGroupBuyId() : null;
    }
}
