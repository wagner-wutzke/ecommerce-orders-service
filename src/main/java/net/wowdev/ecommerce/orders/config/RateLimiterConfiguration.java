package net.wowdev.ecommerce.orders.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfiguration {

  @Bean
  RateLimiter orderCreationRateLimiter(
      @Value("${app.rate-limiting.orders.limit-for-period}") final int limit,
      @Value("${app.rate-limiting.orders.limit-refresh-period}") final Duration refreshPeriod,
      @Value("${app.rate-limiting.orders.timeout-duration}") final Duration timeout) {
    final RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitForPeriod(limit)
            .limitRefreshPeriod(refreshPeriod)
            .timeoutDuration(timeout)
            .build();
    return RateLimiter.of("orderCreation", config);
  }
}
