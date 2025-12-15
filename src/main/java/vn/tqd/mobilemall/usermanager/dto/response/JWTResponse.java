package vn.tqd.mobilemall.usermanager.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JWTResponse {
    private String accessToken;

    // Nếu  làm refresh token thì bỏ comment dòng dưới
    // private String refreshToken;

    @Builder.Default
    private String type = "Bearer";

    private String id;       // UUID
    private String email;
    private String fullName;
    private String avatarUrl;
    private List<String> roles; // Trả về list String ["ROLE_USER", "ROLE_ADMIN"] cho nhẹ
}