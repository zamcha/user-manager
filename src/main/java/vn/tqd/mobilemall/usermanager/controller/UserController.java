package vn.tqd.mobilemall.usermanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.service.UserService;
import vn.tqd.mobilemall.usermanager.service.impl.UserDetailsImpl;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Quản lý người dùng")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Xem thông tin cá nhân", description = "Ai đăng nhập rồi đều xem được chính mình")
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile()));
    }

    @Operation(summary = "Cập nhật hồ sơ", description = "Tự sửa thông tin của mình")
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công"));
    }

    @Operation(summary = "Đổi mật khẩu", description = "Tự đổi của mình")
    @PatchMapping("/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword( request);
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @Operation(summary = "Lấy danh sách tài khoản", description = "Chỉ Admin mới được xem toàn bộ user")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỈ ADMIN MỚI VÀO ĐƯỢC
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String eRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponse> userResponses = userService.getAllUsers(eRole,keyword,pageable);
        return ResponseEntity.ok(ApiResponse.success(userResponses));
    }
//    @DeleteMapping("/me")
//    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
//        userService.deleteAccount(userDetails.getId());
//        return ResponseEntity.ok("Tài khoản đã bị vô hiệu hóa.");
//    }
    @Operation(summary = "Xóa/Khóa tài khoản người khác", description = "Admin xóa user bất kỳ")
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String userId,@RequestParam(defaultValue = "false") Boolean isActive) {
        userService.deleteAccount(userId,isActive);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa người dùng thành công"));
    }
//    @Operation(summary = "Cập nhật quyền tài khoản")
//    @PatchMapping("/{userId}/modifyrole")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<String>> modifyRole(@PathVariable String userId, @RequestParam Set<String> eRole) {
//        userService.modifyRole(userId,eRole);
//        return  ResponseEntity.ok(ApiResponse.success("Cập nhật thành công"));
//    }
    // ==================================================================
    // API 1: Admin tạo người dùng mới
    // POST /api/v1/users
    // ==================================================================
    @Operation(summary = "Tạo mới người dùng (Admin)", description = "Admin tạo user, kích hoạt ngay lập tức.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin được tạo
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody RegisterRequest request) {
        UserResponse newUser = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(newUser));
    }

    // ==================================================================
    // API 2: Sửa quyền User
    // PATCH /api/v1/users/{userId}/modifyrole?eRole=ROLE_ADMIN,ROLE_MANAGER
    // ==================================================================
    @Operation(summary = "Cập nhật quyền tài khoản")
    @PatchMapping("/{userId}/modifyrole")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> modifyRole(
            @PathVariable String userId,
            @RequestParam Set<String> eRole) {

        userService.modifyRole(userId, eRole);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền thành công"));
    }
}