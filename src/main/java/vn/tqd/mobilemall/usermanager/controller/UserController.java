package vn.tqd.mobilemall.usermanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.tqd.mobilemall.common.api.response.ApiResponse;
import vn.tqd.mobilemall.usermanager.dto.request.ChangePasswordRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.service.UserService;
import vn.tqd.mobilemall.usermanager.service.impl.UserDetailsImpl;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Quản lý người dùng")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Xem thông tin cá nhân", description = "Ai đăng nhập rồi đều xem được chính mình")
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(authentication)));
    }

    @Operation(summary = "Cập nhật hồ sơ", description = "Tự sửa thông tin của mình")
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getId(), request));
    }

    @Operation(summary = "Đổi mật khẩu", description = "Tự đổi của mình")
    @PatchMapping("/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @Operation(summary = "Lấy danh sách tài khoản", description = "Chỉ Admin mới được xem toàn bộ user")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỈ ADMIN MỚI VÀO ĐƯỢC
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(userService.getAllUsers(keyword, pageable));
    }
//    @DeleteMapping("/me")
//    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
//        userService.deleteAccount(userDetails.getId());
//        return ResponseEntity.ok("Tài khoản đã bị vô hiệu hóa.");
//    }
    @Operation(summary = "Xóa/Khóa tài khoản người khác", description = "Admin xóa user bất kỳ")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỈ ADMIN
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.ok("Đã xóa người dùng thành công");
    }
}