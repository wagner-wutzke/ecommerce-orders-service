package net.wowdev.ecommerce.orders.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterConfigurationTest {

  @Test
  void createsOrderLimiterWithConfiguredValues() {
    final RateLimiter limiter =
        new RateLimiterConfiguration()
            .orderCreationRateLimiter(10, Duration.ofSeconds(1), Duration.ZERO);

    assertThat(limiter.getName()).isEqualTo("orderCreation");
    assertThat(limiter.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(10);
    assertThat(limiter.getRateLimiterConfig().getLimitRefreshPeriod())
        .isEqualTo(Duration.ofSeconds(1));
    assertThat(limiter.getRateLimiterConfig().getTimeoutDuration()).isZero();
    for (int permit = 0; permit < 10; permit++) {
      assertThat(limiter.acquirePermission()).isTrue();
    }
    assertThat(limiter.acquirePermission()).isFalse();
  }
}
