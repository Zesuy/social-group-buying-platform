package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Cart}.
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
