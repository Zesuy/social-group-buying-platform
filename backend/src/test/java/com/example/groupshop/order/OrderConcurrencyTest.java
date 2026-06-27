package com.example.groupshop.order;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
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
import com.example.groupshop.order.service.OrderService;
import com.example.groupshop.order.service.StoreOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.groupshop.address.dto.AddressResponse;
import com.example.groupshop.address.dto.CreateAddressRequest;
import com.example.groupshop.address.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real concurrent tests for payment and shipment race conditions.
 *
 * <p>Not annotated with {@code @Transactional} — each thread must operate
 * in its own transaction to exercise the actual race window.
 */
class OrderConcurrencyTest extends ServiceTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StoreOrderService storeOrderService;

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

    private Long buyerUserId;
    private Long leaderUserId;
    private Long groupBuyItemId;
    private Long storeId;

    @BeforeEach
    void setUp() {
        // ── Leader + Store ──────────────────────────────────────────
        User leaderUser = new User();
        leaderUser.setNickname("团长并发");
        leaderUser.setPhone("13800099001");
        leaderUser.setStatus("normal");
        userMapper.insert(leaderUser);
        leaderUserId = leaderUser.getId();

        Leader leader = new Leader();
        leader.setUserId(leaderUser.getId());
        leader.setDisplayName("团长并发");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("并发测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // ── Group buy with plenty of stock ──────────────────────────
        CreateGroupBuyRequest gbRequest = new CreateGroupBuyRequest();
        gbRequest.setTitle("并发测试团购");
        gbRequest.setDeliveryType(DeliveryType.EXPRESS);
        CreateGroupBuyRequest.ItemEntry item = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct inlineProduct = new CreateGroupBuyRequest.InlineProduct();
        inlineProduct.setName("并发商品");
        inlineProduct.setBasePriceAmount(1000L);
        inlineProduct.setStock(999);
        item.setProduct(inlineProduct);
        item.setDisplayName("并发商品");
        item.setGroupPriceAmount(990L);
        item.setGroupStock(999);
        item.setSortOrder(1);
        gbRequest.setItems(List.of(item));

        GroupBuyResponse gbResponse = groupBuyService.createGroupBuy(leaderUser.getId(), gbRequest);
        groupBuyItemId = gbResponse.getItems().get(0).getId();

        // ── Buyer ───────────────────────────────────────────────────
        User buyer = new User();
        buyer.setNickname("买家并发");
        buyer.setPhone("13800099002");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        buyerUserId = buyer.getId();
    }

    @AfterEach
    void tearDown() {
        // Clean up all data created by this test
        memberRelationMapper.delete(null);
        shipmentMapper.delete(null);
        orderMapper.delete(null);
        groupBuyItemMapper.delete(null);
        // store, leader, user etc. will be cleaned by the next setUp in the same DB
    }

    // ── Helper: create and return an unpaid order ────────────────────────

    private OrderResponse createUnpaidOrder(Long buyerId, Long addressId) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setGroupBuyId(groupBuyId());
        req.setAddressId(addressId);
        CreateOrderRequest.OrderItemEntry entry = new CreateOrderRequest.OrderItemEntry();
        entry.setGroupBuyItemId(groupBuyItemId);
        entry.setQuantity(1);
        req.setItems(List.of(entry));
        return orderService.createOrder(buyerId, req);
    }

    private Long groupBuyId() {
        return groupBuyItemMapper.selectById(groupBuyItemId).getGroupBuyId();
    }

    // ── Test 1: Concurrent payment, same order ──────────────────────────

    @Test
    void simulatePay_concurrentSameOrder_shouldOnlyPayOnce() throws Exception {
        // Create address for buyer
        CreateAddressRequest addrReq =
                new CreateAddressRequest();
        addrReq.setReceiverName("买家");
        addrReq.setReceiverPhone("13800099003");
        addrReq.setProvince("浙江省");
        addrReq.setCity("杭州市");
        addrReq.setDistrict("西湖区");
        addrReq.setDetail("某某路");
        AddressResponse addrResp =
                addressService.createAddress(buyerUserId, addrReq);

        OrderResponse order = createUnpaidOrder(buyerUserId, addrResp.getId());

        int threadCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(); // both threads start simultaneously
                    orderService.simulatePay(buyerUserId, order.getId());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).as("Both threads completed within timeout").isTrue();

        // Exactly one thread should have succeeded
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Order is paid exactly once
        Order paid = orderMapper.selectById(order.getId());
        assertThat(paid.getPayStatus()).isEqualTo("paid");
        assertThat(paid.getOrderStatus()).isEqualTo("paid");

        // Stock deducted exactly once
        GroupBuyItem item = groupBuyItemMapper.selectById(groupBuyItemId);
        assertThat(item.getGroupStock()).isEqualTo(998); // 999 - 1
        assertThat(item.getSoldCount()).isEqualTo(1);
    }

    // ── Test 2: Concurrent first-payment upsert, same store ─────────────

    @Test
    void upsertMemberRelation_concurrentFirstPayments_shouldNotConflict() throws Exception {
        // Set up: one buyer makes two concurrent first-time payments to the
        // same store. Both orders are in the same group buy but on different
        // items (isolated stock). Without the atomic UPSERT, both transactions
        // would see relation==null and both attempt INSERT, colliding on
        // uk_member_relations_user_store.
        // The UPSERT ensures both payments succeed and counters accumulate atomically.

        // Create another item in the existing group buy (so both orders share
        // the same groupBuy/store/leader but have isolated stock)
        CreateGroupBuyRequest.ItemEntry item2 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct prod2 = new CreateGroupBuyRequest.InlineProduct();
        prod2.setName("并发商品2");
        prod2.setBasePriceAmount(2000L);
        prod2.setStock(999);
        item2.setProduct(prod2);
        item2.setDisplayName("并发商品2");
        item2.setGroupPriceAmount(1990L);
        item2.setGroupStock(999);
        item2.setSortOrder(2);

        // Use the ORIGINAL group buy's ID to create a second item via update
        // Actually, we need a group buy that already has items. Since group buy
        // was already created with 1 item in setUp, we create a new group buy
        // with 2 items from the start.

        // Re-create a fresh group buy with 2 items
        CreateGroupBuyRequest gb2 = new CreateGroupBuyRequest();
        gb2.setTitle("并发测试团购2");
        gb2.setDeliveryType(DeliveryType.EXPRESS);

        CreateGroupBuyRequest.ItemEntry item1 = new CreateGroupBuyRequest.ItemEntry();
        CreateGroupBuyRequest.InlineProduct prod1 = new CreateGroupBuyRequest.InlineProduct();
        prod1.setName("并发商品A");
        prod1.setBasePriceAmount(1000L);
        prod1.setStock(999);
        item1.setProduct(prod1);
        item1.setDisplayName("并发商品A");
        item1.setGroupPriceAmount(990L);
        item1.setGroupStock(999);
        item1.setSortOrder(1);

        gb2.setItems(List.of(item1, item2));
        GroupBuyResponse gb2Resp = groupBuyService.createGroupBuy(leaderUserId, gb2);
        Long gb2Item1Id = gb2Resp.getItems().get(0).getId();
        Long gb2Item2Id = gb2Resp.getItems().get(1).getId();

        // Create addresses
        CreateAddressRequest addrReq1 = new CreateAddressRequest();
        addrReq1.setReceiverName("买家1");
        addrReq1.setReceiverPhone("13800099004");
        addrReq1.setProvince("浙江省");
        addrReq1.setCity("杭州市");
        addrReq1.setDistrict("西湖区");
        addrReq1.setDetail("某某路1");
        AddressResponse addr1 = addressService.createAddress(buyerUserId, addrReq1);

        CreateAddressRequest addrReq2 = new CreateAddressRequest();
        addrReq2.setReceiverName("买家2");
        addrReq2.setReceiverPhone("13800099005");
        addrReq2.setProvince("浙江省");
        addrReq2.setCity("杭州市");
        addrReq2.setDistrict("西湖区");
        addrReq2.setDetail("某某路2");
        AddressResponse addr2 = addressService.createAddress(buyerUserId, addrReq2);

        // Create two orders — same group buy, different items (no stock overlap)
        Long gbId = gb2Resp.getGroupBuy().getId();
        CreateOrderRequest r1 = new CreateOrderRequest();
        r1.setGroupBuyId(gbId);
        r1.setAddressId(addr1.getId());
        CreateOrderRequest.OrderItemEntry e1 = new CreateOrderRequest.OrderItemEntry();
        e1.setGroupBuyItemId(gb2Item1Id);
        e1.setQuantity(1);
        r1.setItems(List.of(e1));

        CreateOrderRequest r2 = new CreateOrderRequest();
        r2.setGroupBuyId(gbId);
        r2.setAddressId(addr2.getId());
        CreateOrderRequest.OrderItemEntry e2 = new CreateOrderRequest.OrderItemEntry();
        e2.setGroupBuyItemId(gb2Item2Id);
        e2.setQuantity(1);
        r2.setItems(List.of(e2));

        OrderResponse order1 = orderService.createOrder(buyerUserId, r1);
        OrderResponse order2 = orderService.createOrder(buyerUserId, r2);

        // Pay both concurrently — same buyer, same store
        int threadCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                barrier.await();
                orderService.simulatePay(buyerUserId, order1.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        executor.submit(() -> {
            try {
                barrier.await();
                orderService.simulatePay(buyerUserId, order2.getId());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).as("Both threads completed within timeout").isTrue();

        // Both payments should succeed (different orders, isolated stock)
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);

        // Single member relation with accumulated counters
        List<MemberRelation> all = memberRelationMapper.selectList(null);
        assertThat(all).hasSize(1); // one relation for this buyer+store
        MemberRelation mr = all.get(0);
        assertThat(mr.getTotalOrders()).isEqualTo(2);
        assertThat(mr.getTotalOrderAmount()).isEqualTo(2980L); // 990 + 1990
    }

    // ── Test 3: Concurrent shipment, same order ─────────────────────────

    @Test
    void shipOrder_concurrentSameOrder_shouldOnlyShipOnce() throws Exception {
        // Create address + order + pay
        CreateAddressRequest addrReq =
                new CreateAddressRequest();
        addrReq.setReceiverName("买家");
        addrReq.setReceiverPhone("13800099007");
        addrReq.setProvince("浙江省");
        addrReq.setCity("杭州市");
        addrReq.setDistrict("西湖区");
        addrReq.setDetail("某某路");
        AddressResponse addrResp =
                addressService.createAddress(buyerUserId, addrReq);

        OrderResponse order = createUnpaidOrder(buyerUserId, addrResp.getId());
        orderService.simulatePay(buyerUserId, order.getId());

        int threadCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ShipOrderRequest shipReq = ShipOrderRequest.builder()
                .deliveryType(DeliveryType.EXPRESS)
                .build();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    storeOrderService.shipOrder(leaderUserId, order.getId(), shipReq);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).as("Both threads completed within timeout").isTrue();

        // Exactly one thread should have succeeded
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Order is shipped
        Order shipped = orderMapper.selectById(order.getId());
        assertThat(shipped.getOrderStatus()).isEqualTo("shipped");

        // Only one shipment record created
        List<Shipment> shipments = shipmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Shipment>()
                        .eq(Shipment::getOrderId, order.getId()));
        assertThat(shipments).hasSize(1);
    }
}
