package net.wowdev.ecommerce.orders.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.events.CustomerReplicationRequested;
import net.wowdev.ecommerce.domain.events.OrderCreated;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class OrderProducerTest {

  @Test
  void publishesOrderCreatedWithEventIdAsKey() {
    final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    final OrderProducer producer = new OrderProducer(template, "orders.v1");
    final UUID eventId = UUID.randomUUID();
    final OrderCreated event =
        new OrderCreated(
            eventId,
            "TX-1",
            null,
            Instant.parse("2026-01-01T00:00:00Z"),
            OrderProducer.ORIGIN_SERVICE);

    producer.publish(event);

    verify(template).send("orders.v1", eventId.toString(), event);
  }

  @Test
  void publishesCustomerReplicationRequestedWithEventIdAsKey() {
    final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    final OrderProducer producer = new OrderProducer(template, "orders.v1");
    final UUID eventId = UUID.randomUUID();
    final CustomerReplicationRequested event =
        new CustomerReplicationRequested(
            eventId,
            "TX-1",
            null,
            Instant.parse("2026-01-01T00:00:00Z"),
            OrderProducer.ORIGIN_SERVICE);

    producer.publish(event);

    verify(template).send("orders.v1", eventId.toString(), event);
  }
}
