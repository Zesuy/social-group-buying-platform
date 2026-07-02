package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.MemberLevelRule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for {@link MemberLevelRule}.
 */
@Mapper
public interface MemberLevelRuleMapper extends BaseMapper<MemberLevelRule> {

    /**
     * Delete all rules for a store (used during full replacement).
     */
    @Delete("DELETE FROM member_level_rules WHERE store_id = #{storeId}")
    int deleteByStoreId(@Param("storeId") Long storeId);
}
