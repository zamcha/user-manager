package vn.tqd.mobilemall.usermanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;   // Có thể null nếu gửi mail kích hoạt (chưa có userId)
    private String email;
    private String title;
    private String content;
    private String type;     // "EMAIL", "PUSH", etc.
}