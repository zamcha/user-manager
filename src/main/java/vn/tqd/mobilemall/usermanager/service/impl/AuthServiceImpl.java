package vn.tqd.mobilemall.usermanager.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.dto.request.LoginRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.response.JWTResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.mapper.UserMapper;
import vn.tqd.mobilemall.usermanager.repository.RoleRepository;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.AuthService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok tự tạo Constructor cho các field final
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    // private final JwtUtils jwtUtils; // Inject JwtUtils của bạn vào đây

    /**
     * Xử lý Đăng ký tài khoản mới (Local)
     */
    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        // 1. Check trùng Email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Lỗi: Email này đã được sử dụng!");
        }

        // 2. Map DTO -> Entity
        User user = userMapper.toUser(request);

        // 3. Bổ sung các thông tin còn thiếu
        user.setId(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider("LOCAL");
        user.setIsActive(true);
        user.setIsVerified(false); // Đăng ký thường cần xác thực email (nếu có tính năng đó)

        // 4. Xử lý Quyền (Role)
        Set<Role> roles = new HashSet<>();

        // Nếu request không gửi quyền gì cả -> Mặc định là ROLE_USER
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền User trong DB."));
            roles.add(userRole);
        } else {
            // Nếu có gửi quyền (Dành cho Admin tạo user), bạn tự xử lý map từ String -> Role ở đây
            // (Thường API đăng ký công khai sẽ bỏ qua phần này để tránh hack quyền Admin)
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền User."));
            roles.add(userRole);
        }

        user.setRoles(roles);

        // 5. Lưu xuống DB
        userRepository.save(user);
    }

    /**
     * Xử lý Đăng nhập (Local)
     */
    @Override
    public JWTResponse login(LoginRequest request) {
        // 1. Xác thực qua Spring Security
        // Nếu sai pass, hàm này tự ném lỗi BadCredentialsException
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Lưu thông tin vào Context (để các filter sau dùng nếu cần)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Lấy thông tin User đã xác thực (Principal)
        // Lưu ý: UserDetailsImpl là class bạn cần tạo khi implement UserDetailsService
        // Phải ép kiểu về class UserDetailsImpl mà bạn đã tạo
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 4. Sinh JWT Token (Thay bằng code thật của bạn)
        // String jwt = jwtUtils.generateJwtToken(authentication); 
        String jwt = "FAKE_JWT_TOKEN_" + userDetails.getUsername(); // Giả lập

        // 5. Lấy danh sách quyền
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 6. Lấy thông tin mở rộng từ DB (vì UserDetails mặc định chỉ có username/pass)
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return JWTResponse.builder()
                .accessToken(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .build();
    }


    /**
     * Xử lý logic sau khi Login Google thành công:
     * - Nếu chưa có -> Tạo mới
     * - Nếu có rồi -> Update thông tin
     */

}