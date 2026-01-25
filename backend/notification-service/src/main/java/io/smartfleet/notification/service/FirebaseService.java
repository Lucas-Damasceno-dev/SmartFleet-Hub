package io.smartfleet.notification.service;

import io.smartfleet.notification.dto.PushNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) service for push notifications.
 */
@Service
@Slf4j
public class FirebaseService {

    @Value("${notification.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${notification.firebase.credentials-path:}")
    private String credentialsPath;

    /**
     * Send push notification to a device.
     *
     * @param request Push notification request
     * @return Message ID if successful
     */
    public String sendPushNotification(PushNotificationRequest request) {
        log.info("Sending push notification to device: {} - title: {}", 
                maskToken(request.getDeviceToken()), request.getTitle());

        if (!firebaseEnabled) {
            String mockMessageId = "mock_msg_" + System.currentTimeMillis();
            log.info("MOCK MODE: Push notification sent - messageId: {}", mockMessageId);
            log.debug("  Title: {}", request.getTitle());
            log.debug("  Body: {}", request.getBody());
            log.debug("  Data: {}", request.getData());
            return mockMessageId;
        }

        // TODO: Implement actual Firebase integration
        // Message message = Message.builder()
        //     .setToken(request.getDeviceToken())
        //     .setNotification(Notification.builder()
        //         .setTitle(request.getTitle())
        //         .setBody(request.getBody())
        //         .build())
        //     .putAllData(request.getData())
        //     .build();
        // return FirebaseMessaging.getInstance().send(message);

        throw new UnsupportedOperationException("Firebase not configured");
    }

    /**
     * Send ORDER_ASSIGNED notification.
     */
    public void sendOrderAssignedNotification(String deviceToken, String driverName, Long orderId) {
        PushNotificationRequest request = PushNotificationRequest.builder()
                .deviceToken(deviceToken)
                .title("Driver Assigned")
                .body("Driver " + driverName + " is on the way!")
                .data(Map.of(
                        "type", "ORDER_ASSIGNED",
                        "orderId", orderId.toString(),
                        "driverName", driverName
                ))
                .build();

        sendPushNotification(request);
    }

    /**
     * Send NEARBY notification (driver within 200m).
     */
    public void sendNearbyNotification(String deviceToken, Long orderId) {
        PushNotificationRequest request = PushNotificationRequest.builder()
                .deviceToken(deviceToken)
                .title("Driver Arriving")
                .body("Driver is arriving (within 200m)")
                .data(Map.of(
                        "type", "NEARBY",
                        "orderId", orderId.toString()
                ))
                .build();

        sendPushNotification(request);
    }

    /**
     * Send DELIVERED notification.
     */
    public void sendDeliveredNotification(String deviceToken, Long orderId) {
        PushNotificationRequest request = PushNotificationRequest.builder()
                .deviceToken(deviceToken)
                .title("Package Delivered")
                .body("Package delivered. Rate your experience.")
                .data(Map.of(
                        "type", "DELIVERED",
                        "orderId", orderId.toString()
                ))
                .build();

        sendPushNotification(request);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
