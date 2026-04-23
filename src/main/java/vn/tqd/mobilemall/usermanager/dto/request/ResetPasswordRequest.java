package vn.tqd.mobilemall.usermanager.dto.request;

import jakarta.validation.constraints.NotBlank;

@lombok.Data
public class ResetPasswordRequest implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @NotBlank
    private String token;
    @NotBlank(message = "Mật khẩu không được để trống")
    private String newPassword;
}
