package com.gallery.platform.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class RealtimeService {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void deviceStatusChanged(Long deviceId, String status, String reason) {
        publish("/topic/devices", Map.of(
                "type", "DEVICE_STATUS",
                "deviceId", deviceId,
                "status", status,
                "reason", reason,
                "at", Instant.now().toString()
        ));
        publish("/topic/devices/" + deviceId, Map.of(
                "type", "DEVICE_STATUS",
                "deviceId", deviceId,
                "status", status,
                "reason", reason,
                "at", Instant.now().toString()
        ));
    }

    public void galleryUpdated(Long deviceId, String change, Object payload) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("type", "GALLERY_UPDATED");
        event.put("deviceId", deviceId);
        event.put("change", change);
        event.put("payload", payload);
        event.put("at", Instant.now().toString());
        publish("/topic/devices/" + deviceId + "/gallery", event);
        publish("/topic/gallery", event);
    }

    public void operationUpdated(Long deviceId, Long operationId, String status, String message) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("type", "OPERATION_UPDATED");
        event.put("deviceId", deviceId);
        event.put("operationId", operationId);
        event.put("status", status);
        event.put("message", message);
        event.put("at", Instant.now().toString());
        publish("/topic/devices/" + deviceId + "/operations", event);
        publish("/topic/operations", event);
    }

    private void publish(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ignored) {
            // realtime is best-effort; core state is persisted
        }
    }
}
