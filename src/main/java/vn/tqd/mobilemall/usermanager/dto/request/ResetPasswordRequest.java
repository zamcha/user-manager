package vn.tqd.mobilemall.usermanager.dto.request;

import jakarta.validation.constraints.NotBlank;

@lombok.Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank(message = "Mật khẩu không được để trống")
    private String newPassword;
}
