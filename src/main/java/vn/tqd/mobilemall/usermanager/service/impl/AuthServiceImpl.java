package vn.tqd.mobilemall.usermanager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.dto.request.LoginRequest;
import vn.tqd.mobilemall.usermanager.dto.request.NotificationRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;
import vn.tqd.mobilemall.usermanager.dto.response.JWTResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.entity.VerificationToken;
import vn.tqd.mobilemall.usermanager.exception.ResourceNotFoundException;
import vn.tqd.mobilemall.usermanager.mapper.UserMapper;
import vn.tqd.mobilemall.usermanager.repository.RoleRepository;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.repository.VerificationTokenRepository;
import vn.tqd.mobilemall.usermanager.service.AuthService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok tự tạo Constructor cho các field final
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final VerificationTokenRepository tokenRepository;
    private final RabbitTemplate rabbitTemplate; // Để bắn email

    // 2. Lấy giá trị từ file yml
    @Value("${queue.notification.exchange}")
    private String exchange;

    @Value("${queue.notification.routing-key}")
    private String routingKey;
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
        user.setIsActive(false);
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
        User savedUser= userRepository.save(user);
        // Tạo Token kích hoạt (Hết hạn sau 24h)
        String token = UUID.randomUUID().toString();
        saveToken(savedUser, token, "REGISTER", 24 * 60);
        String userId = savedUser.getId();
        // Gửi Email
        String link = "http://localhost:5173/verify-account?token=" + token;
        sendEmail(userId,savedUser.getEmail(), "Kích hoạt tài khoản", "Link kích hoạt của bạn: " + link);
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
    // --- 2. HÀM QUÊN MẬT KHẨU ---
    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại"));

        // Xóa token cũ nếu có (để tránh rác DB)
        tokenRepository.findByUserAndType(user, "RESET_PASSWORD")
                .ifPresent(tokenRepository::delete);

        // Tạo Token reset (Hết hạn sau 15 phút)
        String token = UUID.randomUUID().toString();
        saveToken(user, token, "RESET_PASSWORD", 15);

        // Gửi Email
        String link = "http://localhost:5173/reset-password?token=" + token;
        sendEmail(user.getId(),user.getEmail(), "Đặt lại mật khẩu", "Bấm vào đây để đặt lại mật khẩu: " + link);
    }

    // --- 3. HÀM ĐẶT LẠI MẬT KHẨU (User nhập pass mới) ---
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken vToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));

        if (vToken.isExpired() || !vToken.getType().equals("RESET_PASSWORD")) {
            throw new IllegalArgumentException("Token đã hết hạn hoặc không đúng loại");
        }

        User user = vToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa token sau khi dùng xong
        tokenRepository.delete(vToken);
    }

    // --- 4. HÀM KÍCH HOẠT TÀI KHOẢN ---
    @Override
    @Transactional
    public void verifyAccount(String token) {
        VerificationToken vToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token kích hoạt không hợp lệ"));

        if (vToken.isExpired() || !vToken.getType().equals("REGISTER")) {
            throw new IllegalArgumentException("Token đã hết hạn");
        }

        User user = vToken.getUser();
        user.setIsActive(true); // <--- KÍCH HOẠT Ở ĐÂY
        user.setIsVerified(true);
        userRepository.save(user);

        tokenRepository.delete(vToken);
    }

    // --- Helper Methods ---
    private void saveToken(User user, String token, String type, long expiryMinutes) {
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .type(type)
                .expiryDate(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        tokenRepository.save(verificationToken);
    }

    private void sendEmail(String userId,String toEmail, String subject, String content) {
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(userId)
                    .email(toEmail)
                    .title(subject)
                    .content(content)
                    .type("EMAIL")
                    .build();

            // Gửi message lên RabbitMQ
            rabbitTemplate.convertAndSend(exchange, routingKey, request);

            log.info("Đã gửi yêu cầu gửi mail tới Queue cho email: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi RabbitMQ: {}", e.getMessage());
            // Tùy nghiệp vụ: Có thể throw lỗi để rollback transaction tạo user nếu mail lỗi
        }
    }
}