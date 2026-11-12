package net.wowdev.ecommerce.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersistenceConfigurationTest {
  @Test
  void createsPersistenceConfiguration() {
    assertThat(new PersistenceConfiguration()).isNotNull();
  }
}
