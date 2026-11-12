package net.wowdev.ecommerce.orders.controller;

import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.orders.service.OrderNotFoundException;
import net.wowdev.ecommerce.orders.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderController {

  private final OrderService service;
  private final RateLimiter orderCreationRateLimiter;

  public OrderController(final OrderService service, final RateLimiter orderCreationRateLimiter) {
    this.service = service;
    this.orderCreationRateLimiter = orderCreationRateLimiter;
  }

  @GetMapping("/{id}")
  public OrderDTO get(@PathVariable final UUID id) {
    return service.findById(id);
  }

  @GetMapping
  public Page<OrderDTO> list(
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int pageSize) {
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new IllegalArgumentException("Invalid pagination");
    }
    return service.findAll(
        PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @PostMapping
  public ResponseEntity<OrderDTO> create(@Valid @RequestBody final OrderDTO order) {
    if (!orderCreationRateLimiter.acquirePermission()) {
      log.debug(">> Requests rate limit exceeded");
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    OrderDTO created = service.create(order);
    return ResponseEntity.created(URI.create("/api/v1/orders/" + created.getId())).body(created);
  }

  @PutMapping("/{id}")
  public OrderDTO update(@PathVariable final UUID id, @Valid @RequestBody final OrderDTO order) {
    return service.update(id, order);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable final UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<String> notFound(final OrderNotFoundException ex) {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> badRequest(final IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}
