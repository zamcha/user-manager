package vn.tqd.mobilemall.usermanager.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
}