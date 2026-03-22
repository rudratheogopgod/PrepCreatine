package com.prepcreatine.service;

import com.prepcreatine.domain.Notification;
import com.prepcreatine.dto.response.NotificationResponse;
import com.prepcreatine.dto.response.PageResponse;
import com.prepcreatine.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Notification service per BSDD §15.
 * Notifications are created by CommunityService, MentorService, etc.
 * Users can list and mark-all-read.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifRepo;

    public NotificationService(NotificationRepository notifRepo) {
        this.notifRepo = notifRepo;
    }

    public void createNotification(UUID userId, String type, String title, String body, String actionUrl) {
        try {
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType(type);
            notif.setTitle(title);
            notif.setBody(body);
            notif.setActionUrl(actionUrl);
            notifRepo.save(notif);
        } catch (Exception e) {
            log.error("[Notification] Failed to create for userId={}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(UUID userId, Pageable pageable) {
        return PageResponse.from(
            notifRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notifRepo.countByUserIdAndIsReadFalse(userId);
    }

    public void markAllRead(UUID userId) {
        notifRepo.markAllReadForUser(userId);
    }

    public void markRead(UUID notifId, UUID userId) {
        notifRepo.findByIdAndUserId(notifId, userId).ifPresent(n -> {
            n.setRead(true);
            notifRepo.save(n);
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
            n.getId(), n.getType(), n.getTitle(), n.getBody(),
            n.isRead(), n.getActionUrl(), n.getCreatedAt());
    }
}
