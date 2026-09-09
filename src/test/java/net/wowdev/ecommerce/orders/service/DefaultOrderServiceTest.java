package net.wowdev.ecommerce.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.OrderLineDTO;
import net.wowdev.ecommerce.domain.entity.OrderEntity;
import net.wowdev.ecommerce.domain.enums.OrderStatus;
import net.wowdev.ecommerce.domain.events.CustomerReplicationRequested;
import net.wowdev.ecommerce.domain.events.OrderCreated;
import net.wowdev.ecommerce.orders.TestFixtures;
import net.wowdev.ecommerce.orders.messaging.OrderProducer;
import net.wowdev.ecommerce.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DefaultOrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OrderProducer orderProducer;

  private DefaultOrderService service;

  @BeforeEach
  void setUp() {
    service = new DefaultOrderService(orderRepository, orderProducer);
    ReflectionTestUtils.setField(service, "vatRate", 0.15D);
    ReflectionTestUtils.setField(service, "shippingCost", 12.90D);
  }

  @Test
  void findsByIdAndFindsAll() {
    final OrderEntity entity = TestFixtures.orderEntity();
    when(orderRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(orderRepository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    assertThat(service.findById(entity.getId()).getOrderNumber()).isEqualTo("ORD-1");
    assertThat(service.findAll(PageRequest.of(0, 10))).hasSize(1);
  }

  @Test
  void throwsWhenFindingMissingOrder() {
    final UUID id = UUID.randomUUID();
    when(orderRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(id))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found: " + id);
  }

  @Test
  void createsOrderWithExistingIdAndPublishesEvent() {
    final OrderDTO order = TestFixtures.orderDto();
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final OrderDTO result = service.create(order);

    assertThat(result).usingRecursiveComparison().isEqualTo(order);
    final ArgumentCaptor<OrderCreated> event =
        ArgumentCaptor.forClass(OrderCreated.class);
    verify(orderProducer).publish(event.capture());
    assertThat(event.getValue().orderDTO()).usingRecursiveComparison().isEqualTo(order);
    assertThat(event.getValue().transactionId()).isEqualTo(order.getId().toString());
  }

  @Test
  void createsOrderWithGeneratedId() {
    final OrderDTO order = TestFixtures.orderDto();
    order.setId(null);
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final OrderDTO result = service.create(order);

    assertThat(result.getId()).isNotNull();
    assertThat(order.getId()).isEqualTo(result.getId());
    verify(orderProducer).publish(any(OrderCreated.class));
    verify(orderProducer).publish(any(CustomerReplicationRequested.class));
  }

  @Test
  void calculatesAmountsAndAssignsIdsToNewOrderLines() {
    final OrderDTO order = TestFixtures.orderDto();
    order.setId(null);
    final OrderLineDTO line = new OrderLineDTO();
    line.setQuantity(2);
    line.setPrice(new java.math.BigDecimal("10.00"));
    order.setOrderLines(List.of(line));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final OrderDTO result = service.create(order);

    assertThat(result.getOrderAmount()).isEqualByComparingTo("20.00");
    assertThat(result.getShippingAmount()).isEqualByComparingTo("12.90");
    assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
    assertThat(result.getTaxAmount()).isEqualByComparingTo("3.00");
    assertThat(result.getTotalAmount()).isEqualByComparingTo("35.90");
    assertThat(line.getId()).isNotNull();
    assertThat(line.getOrderId()).isEqualTo(result.getId());
    verify(orderProducer).publish(any(OrderCreated.class));
    verify(orderProducer).publish(any(CustomerReplicationRequested.class));
  }

  @Test
  void cancelsOrderAndPersistsCancelledStatus() {
    final OrderDTO order = TestFixtures.orderDto();
    service.cancel(order, "customer request");

    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    verify(orderRepository).save(any(OrderEntity.class));
  }

  @Test
  void updatesOrderWithoutPublishingReplacementEvent() {
    final OrderEntity current = TestFixtures.orderEntity();
    final OrderDTO replacement = TestFixtures.orderDto();
    replacement.setOrderNumber("ORD-2");
    when(orderRepository.findById(current.getId())).thenReturn(Optional.of(current));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final OrderDTO result = service.update(current.getId(), replacement);

    assertThat(result.getId()).isEqualTo(current.getId());
    assertThat(result.getOrderNumber()).isEqualTo("ORD-2");
    verify(orderProducer, never()).publish(any(OrderCreated.class));
  }

  @Test
  void rejectsUpdateForMissingOrder() {
    final UUID id = UUID.randomUUID();
    when(orderRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(id, TestFixtures.orderDto()))
        .isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void deletesExistingOrder() {
    final UUID id = UUID.randomUUID();
    when(orderRepository.existsById(id)).thenReturn(true);

    service.delete(id);

    verify(orderRepository).deleteById(id);
  }

  @Test
  void rejectsDeleteForMissingOrder() {
    final UUID id = UUID.randomUUID();
    when(orderRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(id)).isInstanceOf(OrderNotFoundException.class);
  }
}
