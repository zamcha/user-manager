package vn.tqd.mobilemall.usermanager.service;


import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import vn.tqd.mobilemall.usermanager.dto.request.ChangePasswordRequest;
import vn.tqd.mobilemall.usermanager.dto.request.CreateAccountRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.User;

public interface UserService {
    UserResponse getMyProfile(Authentication authentication);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    void deleteAccount(String userId);
    User getUserById(String userId);
    PageResponse<UserResponse> getAllUsers(String keyword, Pageable pageable);
}