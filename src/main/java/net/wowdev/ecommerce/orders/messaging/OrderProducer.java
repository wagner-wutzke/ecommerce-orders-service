package net.wowdev.ecommerce.orders.messaging;

import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.CustomerReplicationRequested;
import net.wowdev.ecommerce.domain.events.OrderCreated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderProducer {
  public static final String ORIGIN_SERVICE = "ORDERS-SERVICE";
  private final KafkaTemplate<String, Object> template;
  private final String ordersTopic;

  public OrderProducer(
      final KafkaTemplate<String, Object> template,
      @Value("${app.kafka.orders-topic}") final String ordersTopic) {
    this.template = template;
    this.ordersTopic = ordersTopic;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(final OrderCreated event) {
    log.debug(">> Publishing OrderCreated event with event id: {}", event.eventId());
    template.send(ordersTopic, event.eventId().toString(), event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(CustomerReplicationRequested event) {
    log.debug(">> Publishing CustomerReplicationRequested: {}", event.eventId());
    template.send(ordersTopic, event.eventId().toString(), event);
  }
}
