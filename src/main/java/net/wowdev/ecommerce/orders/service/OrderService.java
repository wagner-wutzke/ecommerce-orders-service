package net.wowdev.ecommerce.orders.service;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface OrderService {
  OrderDTO findById(UUID id);

  Page<OrderDTO> findAll(Pageable pageable);

  OrderDTO create(OrderDTO order);

  OrderDTO update(UUID id, OrderDTO order);

  void delete(UUID id);

  void cancel(final OrderDTO orderDTO, final String reason);

  @Transactional
  void complete(OrderDTO orderDTO);
}
