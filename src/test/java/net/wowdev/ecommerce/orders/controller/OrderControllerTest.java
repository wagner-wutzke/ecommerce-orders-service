package net.wowdev.ecommerce.orders.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.github.resilience4j.ratelimiter.RateLimiter;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.orders.TestFixtures;
import net.wowdev.ecommerce.orders.service.OrderNotFoundException;
import net.wowdev.ecommerce.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private OrderService service;
  @MockitoBean private RateLimiter orderCreationRateLimiter;

  @Test
  void supportsCrudEndpoints() throws Exception {
    final OrderDTO order = TestFixtures.orderDto();
    when(service.findById(order.getId())).thenReturn(order);
    when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(order)));
    when(service.create(any())).thenReturn(order);
    when(orderCreationRateLimiter.acquirePermission()).thenReturn(true);
    when(service.update(any(), any())).thenReturn(order);
    doNothing().when(service).delete(order.getId());

    mockMvc.perform(get("/api/v1/orders/{id}", order.getId())).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + order.getId() + "\",\"orderNumber\":\"ORD-1\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/orders/" + order.getId()));
    mockMvc
        .perform(
            put("/api/v1/orders/{id}", order.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNumber\":\"ORD-1\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(delete("/api/v1/orders/{id}", order.getId())).andExpect(status().isNoContent());
  }

  @Test
  void rejectsOrderCreationWhenRateLimitIsExhausted() throws Exception {
    when(orderCreationRateLimiter.acquirePermission()).thenReturn(false);

    mockMvc
        .perform(
            post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNumber\":\"ORD-1\"}"))
        .andExpect(status().isTooManyRequests());

    verify(service, never()).create(any());
  }

  @Test
  void rejectsInvalidPaginationAndHandlesErrors() throws Exception {
    mockMvc.perform(get("/api/v1/orders?page=-1")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/orders?pageSize=0")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/orders?pageSize=101")).andExpect(status().isBadRequest());

    final OrderController controller = new OrderController(service, orderCreationRateLimiter);
    assertThat(controller.notFound(new OrderNotFoundException("missing")).getStatusCode().value())
        .isEqualTo(404);
    assertThat(controller.badRequest(new IllegalArgumentException("bad")).getStatusCode().value())
        .isEqualTo(400);
  }
}
