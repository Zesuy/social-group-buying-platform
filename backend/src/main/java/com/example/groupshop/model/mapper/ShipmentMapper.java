package com.example.groupshop.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.groupshop.model.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for {@link Shipment}.
 */
@Mapper
public interface ShipmentMapper extends BaseMapper<Shipment> {
}
