package vn.tqd.mobilemall.usermanager.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.dto.request.ChangePasswordRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.mapper.UserMapper;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 1. XEM THÔNG TIN (Get Profile)
    @Override
    public UserResponse getMyProfile(Authentication authentication) {
        String email = "";

        // Trường hợp 1: Login Local (UserDetailsImpl)
        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            email = userDetails.getUsername(); // Trả về email
        }
        // Trường hợp 2: Login Google (OAuth2User)
        else if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
        }

        // Gọi Service lấy profile theo Email (Bạn cần sửa Service để tìm theo Email thay vì ID)
        // Hoặc tìm user trong DB để lấy ID rồi gọi hàm cũ
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toUserResponse(user);
    }

    // 2. CHỈNH SỬA THÔNG TIN (Update Profile)
    @Override
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        // Dùng MapStruct update các field có trong request vào entity User
        // Nếu request field nào null, MapStruct sẽ bỏ qua (cần config nullValuePropertyMappingStrategy)
        // Hoặc đơn giản là nó sẽ ghi đè.
        userMapper.updateUserFromRequest(request, user);

        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponse(updatedUser);
    }

    // 3. ĐỔI MẬT KHẨU (Change Password)
    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        // Kiểm tra user này có password không (User Login Google sẽ không có pass)
        if (user.getPassword() == null) {
            throw new RuntimeException("Tài khoản đăng nhập bằng Google/Facebook không thể đổi mật khẩu.");
        }

        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 4. XÓA TÀI KHOẢN (Soft Delete)
    // Hệ thống lớn không bao giờ xóa thật (Hard Delete) vì sẽ mất lịch sử đơn hàng
    @Override
    @Transactional
    public void deleteAccount(String userId) {
        User user = getUserById(userId);

        user.setIsActive(false); // Khóa tài khoản

        // (Optional) Có thể thêm logic: Hủy các token đang hoạt động của user này

        userRepository.save(user);
    }

    // Hàm phụ trợ để tìm user hoặc ném lỗi
    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
    }
    // 2. Hàm cho Admin xem tất cả (Dùng Pageable để phân trang)
    @Override
    public PageResponse<UserResponse> getAllUsers(String keyword, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(keyword, pageable);

        // Map từ Entity sang DTO
        List<UserResponse> list = page.getContent().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .data(list)
                .build();
    }
}