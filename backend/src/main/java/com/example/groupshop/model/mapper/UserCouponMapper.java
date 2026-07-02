package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Mapper for {@link UserCoupon}.
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * Lock a user coupon for an order (unused → locked).
     * Atomic update to prevent concurrent locking.
     *
     * @param id       user coupon ID
     * @param orderId  order ID
     * @param now      current timestamp
     * @return number of rows updated (0 = already locked or not unused)
     */
    @Update("UPDATE user_coupons SET status = 'locked', locked_order_id = #{orderId}, " +
            "locked_at = #{now} WHERE id = #{id} AND status = 'unused'")
    int lockForOrder(@Param("id") Long id, @Param("orderId") Long orderId, @Param("now") LocalDateTime now);

    /**
     * Release a locked user coupon back to unused (locked → unused).
     *
     * @param orderId  order ID
     * @param now      current timestamp
     * @return number of rows updated
     */
    @Update("UPDATE user_coupons SET status = 'unused', locked_order_id = NULL, " +
            "locked_at = NULL WHERE locked_order_id = #{orderId} AND status = 'locked'")
    int releaseLock(@Param("orderId") Long orderId, @Param("now") LocalDateTime now);

    /**
     * Mark a locked user coupon as used (locked → used).
     * Prevents double-use by checking status = 'locked'.
     *
     * @param orderId  order ID
     * @param now      current timestamp
     * @return number of rows updated
     */
    @Update("UPDATE user_coupons SET status = 'used', used_at = #{now} " +
            "WHERE locked_order_id = #{orderId} AND status = 'locked'")
    int markUsed(@Param("orderId") Long orderId, @Param("now") LocalDateTime now);

    /**
     * Mark expired user coupons (unused + past expired_at).
     *
     * @param now current timestamp
     * @return number of rows updated
     */
    @Update("UPDATE user_coupons SET status = 'expired' " +
            "WHERE status = 'unused' AND expired_at IS NOT NULL AND expired_at <= #{now}")
    int batchExpire(@Param("now") LocalDateTime now);
}
