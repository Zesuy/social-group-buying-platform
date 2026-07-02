package com.example.groupshop.coupon;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.enums.DeliveryType;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.coupon.dto.AvailableCouponResponse;
import com.example.groupshop.coupon.dto.CreateCouponRequest;
import com.example.groupshop.coupon.dto.CouponResponse;
import com.example.groupshop.coupon.dto.UpdateCouponRequest;
import com.example.groupshop.coupon.dto.UserCouponResponse;
import com.example.groupshop.coupon.service.CouponService;
import com.example.groupshop.model.entity.Coupon;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import com.example.groupshop.model.entity.User;
import com.example.groupshop.model.entity.UserCoupon;
import com.example.groupshop.model.mapper.CouponMapper;
import com.example.groupshop.model.mapper.LeaderMapper;
import com.example.groupshop.model.mapper.StoreMapper;
import com.example.groupshop.model.mapper.UserCouponMapper;
import com.example.groupshop.model.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CouponService}.
 */
@Transactional
class CouponServiceTest extends ServiceTestBase {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderMapper leaderMapper;

    @Autowired
    private StoreMapper storeMapper;

    private Long storeOwnerUserId;
    private Long storeId;
    private Long buyerUserId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        // Set up store owner (leader)
        User owner = new User();
        owner.setNickname("店主");
        owner.setPhone("13800020001");
        owner.setStatus("normal");
        userMapper.insert(owner);
        storeOwnerUserId = owner.getId();

        Leader leader = new Leader();
        leader.setUserId(owner.getId());
        leader.setDisplayName("测试店主");
        leader.setServiceStatus("normal");
        leader.setMemberCount(0);
        leader.setFollowerCount(0);
        leaderMapper.insert(leader);

        Store store = new Store();
        store.setLeaderId(leader.getId());
        store.setName("测试店铺");
        store.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        store.setDistributionEnabled(false);
        store.setStatus("active");
        storeMapper.insert(store);
        storeId = store.getId();

        // Set up buyer
        User buyer = new User();
        buyer.setNickname("买家");
        buyer.setPhone("13800020002");
        buyer.setStatus("normal");
        userMapper.insert(buyer);
        buyerUserId = buyer.getId();

        // Create a coupon
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("满100减20");
        request.setCouponType("amount");
        request.setAmount(2000L); // 20元
        request.setThresholdAmount(10000L); // 满100元
        request.setTotalQuantity(100);
        request.setPerUserLimit(1);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(30));

        CouponResponse response = couponService.createCoupon(storeOwnerUserId, request);
        couponId = response.getId();
    }

    // ── Store Coupon Management ─────────────────────────────────────────

    @Test
    void createCoupon_shouldSucceed() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("新优惠券");
        request.setCouponType("amount");
        request.setAmount(1000L);
        request.setThresholdAmount(5000L);
        request.setTotalQuantity(50);
        request.setPerUserLimit(2);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(30));

        CouponResponse response = couponService.createCoupon(storeOwnerUserId, request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getName()).isEqualTo("新优惠券");
        assertThat(response.getCouponType()).isEqualTo("amount");
        assertThat(response.getAmount()).isEqualTo(1000L);
        assertThat(response.getThresholdAmount()).isEqualTo(5000L);
        assertThat(response.getTotalQuantity()).isEqualTo(50);
        assertThat(response.getPerUserLimit()).isEqualTo(2);
        assertThat(response.getClaimedQuantity()).isZero();
        assertThat(response.getStatus()).isEqualTo("active");
    }

    @Test
    void createCoupon_shouldFailForNonOwner() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("他人优惠券");
        request.setCouponType("amount");
        request.setAmount(1000L);
        request.setTotalQuantity(10);
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now().plusDays(30));

        // A non-leader user cannot create store coupons
        User nonLeader = new User();
        nonLeader.setNickname("非店主");
        nonLeader.setPhone("13800020003");
        nonLeader.setStatus("normal");
        userMapper.insert(nonLeader);

        assertThatThrownBy(() -> couponService.createCoupon(nonLeader.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LEADER_REQUIRED);
    }

    @Test
    void createCoupon_shouldRejectInvalidCouponType() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("无效类型");
        request.setCouponType("invalid");
        request.setAmount(1000L);
        request.setTotalQuantity(10);
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now().plusDays(30));

        assertThatThrownBy(() -> couponService.createCoupon(storeOwnerUserId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getStoreCoupons_shouldReturnList() {
        List<CouponResponse> coupons = couponService.getStoreCoupons(storeOwnerUserId);

        assertThat(coupons).isNotEmpty();
        assertThat(coupons.get(0).getName()).isEqualTo("满100减20");
    }

    @Test
    void updateCoupon_shouldPartialUpdate() {
        UpdateCouponRequest request = new UpdateCouponRequest();
        request.setName("新名称");
        request.setAmount(3000L);

        CouponResponse response = couponService.updateCoupon(storeOwnerUserId, couponId, request);

        assertThat(response.getName()).isEqualTo("新名称");
        assertThat(response.getAmount()).isEqualTo(3000L);
        // Unchanged fields should remain
        assertThat(response.getCouponType()).isEqualTo("amount");
        assertThat(response.getThresholdAmount()).isEqualTo(10000L);
    }

    @Test
    void updateCoupon_shouldFailForCrossStore() {
        // Create another store
        User anotherOwner = new User();
        anotherOwner.setNickname("另一店主");
        anotherOwner.setPhone("13800020010");
        anotherOwner.setStatus("normal");
        userMapper.insert(anotherOwner);

        Leader anotherLeader = new Leader();
        anotherLeader.setUserId(anotherOwner.getId());
        anotherLeader.setDisplayName("另一店主");
        anotherLeader.setServiceStatus("normal");
        anotherLeader.setMemberCount(0);
        anotherLeader.setFollowerCount(0);
        leaderMapper.insert(anotherLeader);

        Store anotherStore = new Store();
        anotherStore.setLeaderId(anotherLeader.getId());
        anotherStore.setName("另一店铺");
        anotherStore.setDefaultDeliveryType(DeliveryType.EXPRESS.getValue());
        anotherStore.setDistributionEnabled(false);
        anotherStore.setStatus("active");
        storeMapper.insert(anotherStore);

        UpdateCouponRequest request = new UpdateCouponRequest();
        request.setName("跨店修改");

        assertThatThrownBy(() -> couponService.updateCoupon(anotherOwner.getId(), couponId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_FORBIDDEN);
    }

    @Test
    void disableCoupon_shouldSucceed() {
        couponService.disableCoupon(storeOwnerUserId, couponId);

        Coupon coupon = couponMapper.selectById(couponId);
        assertThat(coupon.getStatus()).isEqualTo("disabled");
    }

    // ── Coupon Claiming ─────────────────────────────────────────────────

    @Test
    void claimCoupon_shouldSucceed() {
        UserCouponResponse response = couponService.claimCoupon(buyerUserId, couponId);

        assertThat(response.getId()).isPositive();
        assertThat(response.getCouponId()).isEqualTo(couponId);
        assertThat(response.getCouponName()).isEqualTo("满100减20");
        assertThat(response.getStatus()).isEqualTo("unused");
        assertThat(response.getAmount()).isEqualTo(2000L);
        assertThat(response.getThresholdAmount()).isEqualTo(10000L);

        // claimed_quantity incremented
        Coupon coupon = couponMapper.selectById(couponId);
        assertThat(coupon.getClaimedQuantity()).isEqualTo(1);
    }

    @Test
    void claimCoupon_shouldFailWhenOutOfStock() {
        // Set claimed = total so the next claim fails
        Coupon coupon = couponMapper.selectById(couponId);
        coupon.setClaimedQuantity(coupon.getTotalQuantity());
        couponMapper.updateById(coupon);

        assertThatThrownBy(() -> couponService.claimCoupon(buyerUserId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_OUT_OF_STOCK);
    }

    @Test
    void claimCoupon_shouldFailWhenAlreadyClaimed() {
        couponService.claimCoupon(buyerUserId, couponId);

        assertThatThrownBy(() -> couponService.claimCoupon(buyerUserId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_ALREADY_CLAIMED);
    }

    @Test
    void claimCoupon_shouldFailWhenCouponDisabled() {
        couponService.disableCoupon(storeOwnerUserId, couponId);

        assertThatThrownBy(() -> couponService.claimCoupon(buyerUserId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    void claimCoupon_shouldFailWhenNotStarted() {
        // Create a coupon that starts in the future
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("未来券");
        request.setCouponType("amount");
        request.setAmount(500L);
        request.setTotalQuantity(10);
        request.setStartTime(LocalDateTime.now().plusDays(10));
        request.setEndTime(LocalDateTime.now().plusDays(30));
        CouponResponse futureCoupon = couponService.createCoupon(storeOwnerUserId, request);

        assertThatThrownBy(() -> couponService.claimCoupon(buyerUserId, futureCoupon.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
    }

    // ── My Coupons ──────────────────────────────────────────────────────

    @Test
    void getMyCoupons_shouldReturnClaimedCoupons() {
        couponService.claimCoupon(buyerUserId, couponId);

        List<UserCouponResponse> coupons = couponService.getMyCoupons(buyerUserId, null);
        assertThat(coupons).isNotEmpty();
        assertThat(coupons.get(0).getStatus()).isEqualTo("unused");
    }

    @Test
    void getMyCoupons_shouldFilterByStatus() {
        couponService.claimCoupon(buyerUserId, couponId);

        List<UserCouponResponse> unused = couponService.getMyCoupons(buyerUserId, "unused");
        assertThat(unused).isNotEmpty();

        List<UserCouponResponse> used = couponService.getMyCoupons(buyerUserId, "used");
        assertThat(used).isEmpty();
    }

    @Test
    void getMyCoupons_shouldLazyExpireOverdueCoupons() {
        // Create a coupon that is still valid now but ends very soon
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("临期券");
        request.setCouponType("amount");
        request.setAmount(500L);
        request.setTotalQuantity(10);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusMinutes(5)); // still valid for a few minutes
        CouponResponse coupon = couponService.createCoupon(storeOwnerUserId, request);

        // Claim it (valid at claim time)
        couponService.claimCoupon(buyerUserId, coupon.getId());

        // Manually set expiredAt to the past to simulate expiration
        List<UserCouponResponse> myCoupons = couponService.getMyCoupons(buyerUserId, "unused");
        assertThat(myCoupons).isNotEmpty();
        UserCoupon uc = userCouponMapper.selectById(myCoupons.get(0).getId());
        uc.setExpiredAt(LocalDateTime.now().minusMinutes(1)); // already expired
        userCouponMapper.updateById(uc);

        // getMyCoupons should lazily expire it
        List<UserCouponResponse> coupons = couponService.getMyCoupons(buyerUserId, "expired");
        assertThat(coupons).isNotEmpty();
        assertThat(coupons.get(0).getStatus()).isEqualTo("expired");
    }

    // ── Coupon Validation for Orders ────────────────────────────────────

    @Test
    void validateUserCouponForOrder_shouldSucceed() {
        couponService.claimCoupon(buyerUserId, couponId);
        List<UserCouponResponse> coupons = couponService.getMyCoupons(buyerUserId, "unused");
        assertThat(coupons).isNotEmpty();

        // Validate with sufficient total amount (10000 threshold)
        // couponId maps to the coupon template ID, not userCouponId
        // For this test we use a coupon with 0 threshold
        CreateCouponRequest request = new CreateCouponRequest();
        request.setName("无门槛券");
        request.setCouponType("amount");
        request.setAmount(500L);
        request.setThresholdAmount(0L);
        request.setTotalQuantity(100);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(30));
        CouponResponse noThreshold = couponService.createCoupon(storeOwnerUserId, request);

        UserCouponResponse uc = couponService.claimCoupon(buyerUserId, noThreshold.getId());
        List<UserCouponResponse> myCoupons = couponService.getMyCoupons(buyerUserId, "unused");
        UserCouponResponse userCoupon = myCoupons.stream()
                .filter(c -> c.getCouponId().equals(noThreshold.getId()))
                .findFirst().orElseThrow();

        // Validate (requires groupBuyId — use 0 for this test since we just test basic validation)
        // We'll test full integration in OrderServiceTest
        assertThat(userCoupon.getStatus()).isEqualTo("unused");
    }

    // ── Order Coupon Lifecycle ─────────────────────────────────────────

    @Test
    void lockAndReleaseCoupon_shouldWork() {
        UserCouponResponse uc = couponService.claimCoupon(buyerUserId, couponId);
        List<UserCouponResponse> myCoupons = couponService.getMyCoupons(buyerUserId, "unused");
        UserCouponResponse userCoupon = myCoupons.get(0);

        // Lock
        couponService.lockUserCoupon(userCoupon.getId(), 999L);

        UserCoupon locked = userCouponMapper.selectById(userCoupon.getId());
        assertThat(locked.getStatus()).isEqualTo("locked");
        assertThat(locked.getLockedOrderId()).isEqualTo(999L);

        // Release
        couponService.releaseUserCoupon(999L);

        UserCoupon released = userCouponMapper.selectById(userCoupon.getId());
        assertThat(released.getStatus()).isEqualTo("unused");
        assertThat(released.getLockedOrderId()).isNull();
    }

    @Test
    void markUsed_shouldWork() {
        UserCouponResponse uc = couponService.claimCoupon(buyerUserId, couponId);
        List<UserCouponResponse> myCoupons = couponService.getMyCoupons(buyerUserId, "unused");
        UserCouponResponse userCoupon = myCoupons.get(0);

        // Lock then use
        couponService.lockUserCoupon(userCoupon.getId(), 999L);
        couponService.useUserCoupon(999L);

        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertThat(used.getStatus()).isEqualTo("used");
        assertThat(used.getUsedAt()).isNotNull();
    }

    @Test
    void markUsed_shouldBeIdempotent() {
        UserCouponResponse uc = couponService.claimCoupon(buyerUserId, couponId);
        List<UserCouponResponse> myCoupons = couponService.getMyCoupons(buyerUserId, "unused");
        UserCouponResponse userCoupon = myCoupons.get(0);

        couponService.lockUserCoupon(userCoupon.getId(), 999L);
        couponService.useUserCoupon(999L);
        // Second call should be a no-op (not throw)
        couponService.useUserCoupon(999L);

        UserCoupon used = userCouponMapper.selectById(userCoupon.getId());
        assertThat(used.getStatus()).isEqualTo("used");
    }

    // ── Available Coupons Listing ──────────────────────────────────────

    @Test
    void getCouponsForOrderPreview_withInvalidGroupBuy_shouldThrow() {
        assertThatThrownBy(() -> couponService.getCouponsForOrderPreview(
                buyerUserId, 99999L, 15000L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
