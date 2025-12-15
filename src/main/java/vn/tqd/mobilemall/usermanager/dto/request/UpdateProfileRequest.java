package vn.tqd.mobilemall.usermanager.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
}