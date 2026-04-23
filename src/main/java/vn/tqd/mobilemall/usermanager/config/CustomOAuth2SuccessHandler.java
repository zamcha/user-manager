package vn.tqd.mobilemall.usermanager.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.repository.RoleRepository;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.AuthService;
import vn.tqd.mobilemall.usermanager.service.impl.UserDetailsImpl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor // Lombok tự inject AuthService
@Slf4j
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();
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
        // Lấy roles từ DB
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        // Tạo UserDetailsImpl
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Gắn lại Authentication với roles từ DB
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        // --- Logic sinh Token và Redirect (Giữ nguyên) ---
        // String token = jwtUtils.generateTokenFromUser(user);

        // 2. Kiểm tra xem trước khi login, user có đang muốn đi đâu không?
        // (Ví dụ: Đang muốn vào /oauth2/authorize để xin quyền)
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest != null) {
            // Nếu có link cũ, ưu tiên quay lại đó (để hiện Consent Screen)
            String targetUrl = savedRequest.getRedirectUrl();
            log.info("Redirecting to saved request: {}", targetUrl);
            response.sendRedirect(targetUrl);
        } else {
            // Nếu không có (Login chủ động), thì về trang chủ React
            log.info("No saved request, redirecting to Home");
            response.sendRedirect("http://localhost:5173/");
        }
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