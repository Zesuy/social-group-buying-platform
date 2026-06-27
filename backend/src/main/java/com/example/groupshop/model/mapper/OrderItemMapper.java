package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * Minimal mapper for {@link OrderItem} — used only for group buy item protection checks.
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
