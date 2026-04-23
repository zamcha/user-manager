package vn.tqd.mobilemall.usermanager.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import vn.tqd.mobilemall.usermanager.dto.request.ChangePasswordRequest;
import vn.tqd.mobilemall.usermanager.dto.request.CreateAccountRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.User;

import java.util.Set;

public interface UserService {
    UserResponse getMyProfile();
    void updateProfile( UpdateProfileRequest request);
    void changePassword(ChangePasswordRequest request);
    void deleteAccount(String userId, Boolean isActive);

    UserResponse getUserByIdSync(String userId);
    Page<UserResponse> getAllUsers(String eRole, String keyword, Pageable pageable);
    void modifyRole(String userId, Set<String> roleEnums);
    UserResponse createUser(RegisterRequest request);
}