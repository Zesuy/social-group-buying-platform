package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Store;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Store}.
 */
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
