package net.wowdev.ecommerce.orders.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.entity.OrderEntity;
import net.wowdev.ecommerce.domain.enums.OrderStatus;
import net.wowdev.ecommerce.domain.events.CustomerReplicationRequested;
import net.wowdev.ecommerce.domain.events.OrderCreated;
import net.wowdev.ecommerce.domain.mapper.OrderMapper;
import net.wowdev.ecommerce.orders.messaging.OrderProducer;
import net.wowdev.ecommerce.orders.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

  private static final String ORIGIN_SERVICE = "ORDERS-SERVICE";

  private final OrderRepository repository;
  private final OrderProducer producer;

  @Value("${business.config.vat-rate}")
  private Double vatRate;

  @Value("${business.config.shipping-cost}")
  private Double shippingCost;

  @Override
  @Transactional(readOnly = true)
  public OrderDTO findById(final UUID id) {
    return repository
        .findById(id)
        .map(OrderMapper::toDto)
        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OrderDTO> findAll(final Pageable pageable) {
    return repository.findAll(pageable).map(OrderMapper::toDto);
  }

  @Override
  @Transactional
  public OrderDTO update(final UUID id, final OrderDTO order) {
    OrderEntity current =
        repository
            .findById(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    OrderDTO replacement = OrderMapper.toDto(current);
    replacement.setOrderNumber(order.getOrderNumber());
    replacement.setOrderStatus(order.getOrderStatus());
    replacement.setTotalAmount(order.getTotalAmount());
    replacement.setShippingAmount(order.getShippingAmount());
    replacement.setOrderLines(order.getOrderLines());
    replacement.setOrderAmount(order.getOrderAmount());
    replacement.setTaxAmount(order.getTaxAmount());
    replacement.setDiscountAmount(order.getDiscountAmount());
    return OrderMapper.toDto(repository.save(OrderMapper.toEntity(replacement)));
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    if (!repository.existsById(id)) {
      throw new OrderNotFoundException("Order not found: " + id);
    }
    repository.deleteById(id);
  }

  @Override
  @Transactional
  public void cancel(OrderDTO orderDTO, String reason) {
    log.debug("Canceling order {} for the reason: {}", orderDTO.getId(), reason);
    orderDTO.setOrderStatus(OrderStatus.CANCELLED);
    repository.save(OrderMapper.toEntity(orderDTO));
  }

  @Override
  @Transactional
  public OrderDTO create(final OrderDTO orderDTO) {
    if (orderDTO.getId() == null) {
      final UUID orderId = UUID.randomUUID();
      orderDTO.setId(orderId);
      orderDTO.setOrderStatus(OrderStatus.CREATED);
      orderDTO
          .getOrderLines()
          .forEach(
              orderLine -> {
                UUID orderLineId = UUID.randomUUID();
                orderLine.setOrderId(orderId);
                orderLine.setId(orderLineId);
              });
    }
    calculateOrderAmounts(orderDTO);

    // notify Customers services to pre-load customer data for this order
    publishCustomerReplicationRequested(orderDTO);

    final OrderEntity savedOrderEntity = repository.save(OrderMapper.toEntity(orderDTO));
    log.debug(">> Created new Order with id: {}", savedOrderEntity.getId());
    final OrderDTO savedOrderDTO = OrderMapper.toDto(savedOrderEntity);

    publishOrderCreated(savedOrderDTO);

    return savedOrderDTO;
  }

  @Transactional
  @Override
  public void complete(final OrderDTO orderDTO) {
    orderDTO.setOrderStatus(OrderStatus.CREATED);
    repository.save(OrderMapper.toEntity(orderDTO));
    log.debug(">> Order process successfully finished: {}. Order status: {}",
        orderDTO.getId(),
        orderDTO.getOrderStatus());
  }

  protected void calculateOrderAmounts(OrderDTO orderDTO) {
    BigDecimal orderAmount =
        orderDTO.getOrderLines().stream()
            .map(line -> line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    orderDTO.setOrderAmount(orderAmount);
    orderDTO.setShippingAmount(BigDecimal.valueOf(shippingCost));
    orderDTO.setDiscountAmount(BigDecimal.ZERO); // TODO implement discount table
    orderDTO.setTaxAmount(BigDecimal.valueOf(vatRate).multiply(orderAmount));
    orderDTO.setTotalAmount(
        orderAmount
            .add(orderDTO.getDiscountAmount())
            .add(orderDTO.getTaxAmount())
            .add(orderDTO.getShippingAmount())
            .add(orderDTO.getDiscountAmount()));
  }

  private void publishOrderCreated(OrderDTO savedOrderDTO) {
    OrderCreated event =
        new OrderCreated(
            UUID.randomUUID(),
            savedOrderDTO.getId().toString(),
            savedOrderDTO,
            Instant.now(),
            OrderProducer.ORIGIN_SERVICE);
    // TODO: persist event before publishing (outbox pattern)
    producer.publish(event);
  }

  protected void publishCustomerReplicationRequested(OrderDTO orderDTO) {
    CustomerReplicationRequested event =
        new CustomerReplicationRequested(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            orderDTO,
            Instant.now(),
            ORIGIN_SERVICE);
    producer.publish(event);
  }

}
