package com.prepcreatine.controller;

import com.prepcreatine.dto.response.*;
import com.prepcreatine.service.NotificationService;
import com.prepcreatine.util.SecurityUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Notification endpoints per BSDD §15.
 *
 * GET    /api/notifications
 * GET    /api/notifications/unread-count
 * PUT    /api/notifications/read-all
 * PUT    /api/notifications/{id}/read
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifService;

    public NotificationController(NotificationService notifService) {
        this.notifService = notifService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notifService.listForUser(SecurityUtil.getCurrentUserId(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        long count = notifService.countUnread(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead() {
        notifService.markAllRead(SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable UUID id) {
        notifService.markRead(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }
}
