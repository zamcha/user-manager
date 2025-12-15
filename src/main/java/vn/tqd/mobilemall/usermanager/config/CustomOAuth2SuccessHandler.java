package vn.tqd.mobilemall.usermanager.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.repository.RoleRepository;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.AuthService;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor // Lombok tự inject AuthService
@Slf4j
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    // private final JwtUtils jwtUtils; // Nếu muốn sinh token

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String avatarUrl = oauth2User.getAttribute("picture");
        String googleId = oauth2User.getAttribute("sub");

        log.info("Google Login Success: {}", email);

        // 👇 GỌI SERVICE ĐỂ XỬ LÝ DB (Ngắn gọn, sạch sẽ)
        User user = processOAuthPostLogin(email, name, avatarUrl, googleId);

        // --- Logic sinh Token và Redirect (Giữ nguyên) ---
        // String token = jwtUtils.generateTokenFromUser(user);

        response.sendRedirect("http://localhost:8888/user-manager/swagger-ui/index.html");
        // Hoặc: response.sendRedirect("http://localhost:3000?token=" + token);
    }

    @Transactional
    public User processOAuthPostLogin(String email, String name, String avatarUrl, String providerId) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            // --- UPDATE USER CŨ ---
            user = userOptional.get();
            // Chỉ update nếu có thay đổi để tối ưu, hoặc update luôn cũng được
            user.setFullName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProviderId(providerId);

            // Nếu user cũ là LOCAL, cập nhật sang GOOGLE để lần sau biết
            if ("LOCAL".equals(user.getAuthProvider())) {
                user.setAuthProvider("GOOGLE");
            }
            user.setIsVerified(true); // Google đã xác thực
        } else {
            // --- TẠO USER MỚI ---
            user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setEmail(email);
            user.setFullName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProviderId(providerId);
            user.setAuthProvider("GOOGLE");
            user.setIsActive(true);
            user.setIsVerified(true);

            // Gán quyền ROLE_USER
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền ROLE_USER"));
            roles.add(userRole);
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }
}