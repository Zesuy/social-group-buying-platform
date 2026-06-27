package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.MemberRelation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * Mapper for {@link MemberRelation}.
 */
@Mapper
public interface MemberRelationMapper extends BaseMapper<MemberRelation> {

    /**
     * Atomic upsert: INSERT ... ON DUPLICATE KEY UPDATE.
     *
     * <p>Safe under concurrent payments from the same user to the same store.
     * On first payment inserts a new row; on subsequent payments atomically
     * increments counters without a read-then-write gap.
     *
     * <p>Unique constraint: {@code uk_member_relations_user_store (user_id, store_id)}.
     *
     * @param userId      buyer user ID
     * @param leaderId    leader ID
     * @param storeId     store ID
     * @param payAmount   amount in fen for this payment
     * @param lastOrderAt timestamp of this payment
     * @return number of rows affected (1 = insert, 2 = update)
     */
    @Insert("""
            INSERT INTO member_relations (user_id, leader_id, store_id, level_name, growth_value, total_order_amount, total_orders, last_order_at, created_at)
            VALUES (#{userId}, #{leaderId}, #{storeId}, 'V0', #{payAmount}, #{payAmount}, 1, #{lastOrderAt}, NOW())
            ON DUPLICATE KEY UPDATE
                total_order_amount = total_order_amount + #{payAmount},
                total_orders = total_orders + 1,
                growth_value = growth_value + #{payAmount},
                last_order_at = #{lastOrderAt},
                leader_id = #{leaderId}
            """)
    int upsert(@Param("userId") Long userId,
               @Param("leaderId") Long leaderId,
               @Param("storeId") Long storeId,
               @Param("payAmount") Long payAmount,
               @Param("lastOrderAt") LocalDateTime lastOrderAt);
}
