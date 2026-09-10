package net.wowdev.ecommerce.orders.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.events.InventoryFailed;
import net.wowdev.ecommerce.orders.service.OrderService;
import org.junit.jupiter.api.Test;

class OrderConsumerTest {

  private final OrderService orderService = mock(OrderService.class);
  private final OrderConsumer consumer = new OrderConsumer(orderService);

  @Test
  void handlesUnknownEvents() {
    consumer.handleUnknown(new Object());
  }

  @Test
  void cancelsOrderWhenInventoryFails() {
    final OrderDTO order = new OrderDTO();
    final InventoryFailed event =
        new InventoryFailed(
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
