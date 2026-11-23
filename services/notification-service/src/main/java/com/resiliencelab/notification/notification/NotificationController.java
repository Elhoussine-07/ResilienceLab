package com.resiliencelab.notification.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping("/orders/{orderId}")
    public List<NotificationResponse> getByOrderId(@PathVariable UUID orderId) { return service.getByOrderId(orderId); }
}