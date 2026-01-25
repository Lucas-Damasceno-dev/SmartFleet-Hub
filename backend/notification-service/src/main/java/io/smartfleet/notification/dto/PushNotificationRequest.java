package io.smartfleet.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for push notification request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationRequest {

    private String deviceToken;
    private String title;
    private String body;
    private Map<String, String> data;
}
