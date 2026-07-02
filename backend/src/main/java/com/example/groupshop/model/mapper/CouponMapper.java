package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for {@link Coupon}.
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * Atomic increment claimed_quantity when a user claims a coupon.
     * Only succeeds if claimed_quantity < total_quantity.
     *
     * @param couponId the coupon ID
     * @return number of rows updated (0 = out of stock or not found)
     */
    @Update("UPDATE coupons SET claimed_quantity = claimed_quantity + 1 " +
            "WHERE id = #{couponId} AND claimed_quantity < total_quantity " +
            "AND status = 'active'")
    int incrementClaimedQuantity(@Param("couponId") Long couponId);

    /**
     * Select coupon with pessimistic lock (FOR UPDATE).
     * Used within {@code @Transactional} to serialize concurrent claims
     * on the same coupon, preventing per-user limit races.
     *
     * @param id the coupon ID
     * @return the coupon row, locked for the duration of the transaction
     */
    @Select("SELECT * FROM coupons WHERE id = #{id} FOR UPDATE")
    Coupon selectForUpdate(@Param("id") Long id);
}
