package net.wowdev.ecommerce.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaConfigurationTest {
  @Test
  void createsConfiguredProducerConsumerAndListenerFactory() {
    final KafkaConfiguration config = new KafkaConfiguration();
    ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
    ReflectionTestUtils.setField(config, "consumerGroup", "orders");
    ReflectionTestUtils.setField(config, "acks", "all");
    ReflectionTestUtils.setField(config, "deliveryTimeout", "30000");
    ReflectionTestUtils.setField(config, "linger", "0");
    ReflectionTestUtils.setField(config, "requestTimeout", "10000");
    ReflectionTestUtils.setField(config, "idempotence", true);
    ReflectionTestUtils.setField(config, "retries", 3);
    ReflectionTestUtils.setField(config, "trustedPackages", "net.wowdev.ecommerce.domain.events");
    ReflectionTestUtils.setField(config, "maxRequestsInFlight", 5);

    final ProducerFactory<String, Object> producerFactory = config.producerFactory();
    final ConsumerFactory<String, Object> consumerFactory = config.consumerFactory();
    final KafkaTemplate<String, Object> template = config.kafkaTemplate(producerFactory);
    final ConcurrentKafkaListenerContainerFactory<String, Object> listenerFactory =
        config.kafkaListenerContainerFactory(consumerFactory, template);

    assertThat(producerFactory).isNotNull();
    assertThat(consumerFactory).isNotNull();
    assertThat(template).isNotNull();
    assertThat(listenerFactory.getConsumerFactory()).isSameAs(consumerFactory);
  }
}
