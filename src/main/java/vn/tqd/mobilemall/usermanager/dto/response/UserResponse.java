package vn.tqd.mobilemall.usermanager.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String avatarUrl;

    private String authProvider; // GOOGLE / LOCAL

    private Boolean isVerified;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    private Set<String> roles; // Chỉ trả về tên quyền (ROLE_USER...)
}