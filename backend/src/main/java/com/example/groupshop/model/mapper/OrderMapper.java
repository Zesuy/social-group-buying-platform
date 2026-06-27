package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * Minimal mapper for {@link Order} — used only for group buy item protection checks.
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
