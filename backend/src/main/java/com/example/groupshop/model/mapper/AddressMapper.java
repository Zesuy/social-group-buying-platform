package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Address}.
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
