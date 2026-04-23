package vn.tqd.mobilemall.usermanager.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.dto.request.ChangePasswordRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.PageResponse;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.exception.ResourceNotFoundException;
import vn.tqd.mobilemall.usermanager.mapper.UserMapper;
import vn.tqd.mobilemall.usermanager.repository.RoleRepository;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;


    // 1. XEM THÔNG TIN (Get Profile)
    @Override
    public UserResponse getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        String email = jwt.getClaim("email");

        // Gọi Service lấy profile theo Email (Bạn cần sửa Service để tìm theo Email thay vì ID)
        // Hoặc tìm user trong DB để lấy ID rồi gọi hàm cũ
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toUserResponse(user);
    }

    // 2. CHỈNH SỬA THÔNG TIN (Update Profile)
    @Override
    @Transactional
    public void updateProfile( UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        User user = getUserById(userId);
        Boolean phoneNumberExists = userRepository.existsByPhoneNumber(request.getPhoneNumber());
        
        // Dùng MapStruct update các field có trong request vào entity User
        // Nếu request field nào null, MapStruct sẽ bỏ qua (cần config nullValuePropertyMappingStrategy)
        // Hoặc đơn giản là nó sẽ ghi đè.
        if(!phoneNumberExists){
        userMapper.updateUserFromRequest(request, user);

        User updatedUser = userRepository.save(user);
        userMapper.toUserResponse(updatedUser);
        }
        
    }

    // 3. ĐỔI MẬT KHẨU (Change Password)
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        User user = getUserById(userId);

        // Kiểm tra user này có password không (User Login Google sẽ không có pass)
        if (user.getPassword() == null) {
            throw new ResourceNotFoundException("Tài khoản đăng nhập bằng Google/Facebook không thể đổi mật khẩu.");
        }

        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Mật khẩu cũ không chính xác!");
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 4. XÓA TÀI KHOẢN (Soft Delete)
    // Hệ thống lớn không bao giờ xóa thật (Hard Delete) vì sẽ mất lịch sử đơn hàng
    @Override
    @Transactional
    public void deleteAccount(String userId, Boolean isActive) {
        User user = getUserById(userId);

        user.setIsActive(isActive); // Khóa tài khoản

        // (Optional) Có thể thêm logic: Hủy các token đang hoạt động của user này

        userRepository.save(user);
    }

    // Hàm phụ trợ để tìm user hoặc ném lỗi

    private User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
    }
    @Override
    public UserResponse getUserByIdSync(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        return userMapper.toUserResponse(user);
    }
    // 2. Hàm cho Admin xem tất cả (Dùng Pageable để phân trang)
    @Override
    public Page<UserResponse> getAllUsers(String eRoleStr, String keyword, Pageable pageable) {
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        // 2. Xử lý Role (String -> Enum)
        ERole roleParam = null;
        if (eRoleStr != null && !eRoleStr.trim().isEmpty()) {
            try {
                // Chuyển chuỗi "ROLE_ADMIN" thành Enum ERole.ROLE_ADMIN
                roleParam = ERole.valueOf(eRoleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Nếu chuỗi gửi lên không đúng (VD: "ROLE_XYZ") -> coi như không lọc
                roleParam = null;
            }
        }
        return  userRepository.searchUsers(roleParam,keyword, pageable)
                .map(userMapper::toUserResponse);
    }

    @Override
    @Transactional
    public void modifyRole(String userId, Set<String> roleEnums) {
        // 1. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 2. Tìm các Role từ Enum gửi lên
        Set<Role> roles   = new HashSet<>();
        if(!roleEnums.isEmpty()&&roleEnums != null){
            roleEnums.forEach(roleStr -> {
                try {
                    ERole eRole = ERole.valueOf(roleStr.toUpperCase());
                    Role role = roleRepository.findByName(eRole)
                            .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + roleStr));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    throw new ResourceNotFoundException("Tên quyền không hợp lệ: " + roleStr);
                }
            });
        }
        // 3. Cập nhật và Lưu
        user.setRoles(roles);
        userRepository.save(user);
    }
    @Override
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        // 1. Kiểm tra Email tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email đã tồn tại trong hệ thống!");
        }

        // 2. Map dữ liệu
        User user = userMapper.toUser(request);
        user.setId(UUID.randomUUID().toString());

        // 3. Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 4. Thiết lập mặc định 
        user.setAuthProvider("LOCAL");
        user.setIsActive(true);   // Admin tạo thì cho kích hoạt luôn
        user.setIsVerified(true); // Bỏ qua bước xác thực email

        // 5. Xử lý Quyền (Role)
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            // Nếu request có gửi quyền lên (VD: ["ROLE_ADMIN", "ROLE_MANAGER"])
            request.getRoles().forEach(roleStr -> {
                try {
                    ERole eRole = ERole.valueOf(roleStr.toUpperCase());
                    Role role = roleRepository.findByName(eRole)
                            .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại: " + roleStr));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    throw new ResourceNotFoundException("Tên quyền không hợp lệ: " + roleStr);
                }
            });
        } else {
            // Mặc định là ROLE_USER nếu không gửi gì
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new ResourceNotFoundException("Lỗi cấu hình: Chưa có ROLE_USER trong DB"));
            roles.add(userRole);
        }
        user.setRoles(roles);

        // 6. Lưu xuống DB
        User savedUser = userRepository.save(user);

        // 7. Trả về Response
        return userMapper.toUserResponse(savedUser);
    }
}