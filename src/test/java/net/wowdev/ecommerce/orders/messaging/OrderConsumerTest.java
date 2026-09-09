package net.wowdev.ecommerce.orders.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.events.InventoryUpdateFailedEvent;
import net.wowdev.ecommerce.domain.events.OrderProcessingCompletedEvent;
import net.wowdev.ecommerce.domain.events.OrderProcessingFailedEvent;
import net.wowdev.ecommerce.orders.service.OrderService;
import org.junit.jupiter.api.Test;

class OrderConsumerTest {

  private final OrderService orderService = mock(OrderService.class);
  private final OrderConsumer consumer = new OrderConsumer(orderService);

  @Test
  void handlesFailedOrders() {
    consumer.handle(mock(OrderProcessingFailedEvent.class));
  }

  @Test
  void handlesCompletedOrders() {
    consumer.handle(mock(OrderProcessingCompletedEvent.class));
  }

  @Test
  void handlesUnknownEvents() {
    consumer.handleUnknown(new Object());
  }

  @Test
  void cancelsOrderWhenInventoryUpdateFails() {
    final OrderDTO order = new OrderDTO();
    final InventoryUpdateFailedEvent event =
        new InventoryUpdateFailedEvent(
            UUID.randomUUID(),
            "transaction-1",
            order,
            "Insufficient stock",
            Instant.parse("2026-01-01T00:00:00Z"),
            "INVENTORY-SERVICE");

    consumer.handle(event);

    verify(orderService).cancel(order, "Insufficient stock");
  }
}
