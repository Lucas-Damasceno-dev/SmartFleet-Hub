package io.smartfleet.order.mapper;

import io.smartfleet.common.dto.OrderDto;
import io.smartfleet.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Order entity/DTO conversion.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderDto toDto(Order entity);

    Order toEntity(OrderDto dto);
}
