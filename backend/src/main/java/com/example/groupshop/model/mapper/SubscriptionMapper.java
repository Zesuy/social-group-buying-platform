package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Subscription}.
 */
@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
