package vn.tqd.mobilemall.usermanager.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import vn.tqd.mobilemall.usermanager.entity.User;
import vn.tqd.mobilemall.usermanager.repository.UserRepository;
import vn.tqd.mobilemall.usermanager.service.impl.UserDetailsImpl;
import vn.tqd.mobilemall.usermanager.service.impl.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Inject Handler xử lý sau khi Google Login thành công
    private final AuthenticationSuccessHandler customOAuth2SuccessHandler;
    private final UserDetailsServiceImpl userDetailsService;

    // Bean 1: Dành cho Authorization Server (Cấp Token) - GIỮ NGUYÊN
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
//
//        http.cors(c -> c.configurationSource(request -> {
//            CorsConfiguration corsConfiguration = new CorsConfiguration();
//            corsConfiguration.addAllowedOrigin("*");
//            corsConfiguration.addAllowedHeader("*");
//            corsConfiguration.addAllowedMethod("*");
//            return corsConfiguration;
//        }));
        http
                .cors(cors -> cors.disable())
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    // Bean 2: Dành cho App Security (Login form, Google Login) - ĐÃ SỬA
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
//        http.cors(c -> c.configurationSource(request -> {
//            CorsConfiguration corsConfiguration = new CorsConfiguration();
//            corsConfiguration.addAllowedOrigin("*");
//            corsConfiguration.addAllowedHeader("*");
//            corsConfiguration.addAllowedMethod("*");
//            return corsConfiguration;
//        }));

        http
                .cors(cors -> cors.disable())
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        // Cho phép truy cập công khai vào các endpoint login
                        .requestMatchers( "/login/**", "/oauth2/**","api/v1/auth/login","api/v1/auth/register","/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/verify-account").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
//                .logout(logout -> logout
//                        .logoutUrl("/api/v1/auth/logout") // 1. Đường dẫn API Logout
//                        .addLogoutHandler(new SecurityContextLogoutHandler()) // 2. Xóa SecurityContext
//                        .logoutSuccessHandler((request, response, authentication) -> {
//                            // 3. Phản hồi JSON khi logout thành công
//                            SecurityContextHolder.clearContext(); // Đảm bảo xóa sạch context
//
//                            response.setStatus(HttpServletResponse.SC_OK);
//                            response.setContentType("application/json");
//                            response.setCharacterEncoding("UTF-8");
//                            response.getWriter().write("{\"message\": \"Đăng xuất thành công!\", \"status\": 200}");
//                        })
//                        .permitAll()
//                )
                // 1. Đăng nhập bằng Form (Username/Password) cũ
                .formLogin(Customizer.withDefaults())

                // 2. THÊM MỚI: Đăng nhập bằng Google
                .oauth2Login(oauth2 -> oauth2
                        // Khi Google login thành công thì chạy vào handler này
                        .successHandler(customOAuth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Endpoint logout mặc định
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Xóa session
                            request.getSession().invalidate();

                            // Quan trọng: Redirect về lại trang Login của Frontend
                            // Do bạn chạy qua Gateway nên set cứng localhost:5173 cho chắc
                            response.sendRedirect("http://localhost:5173/login");
                        })
//                        .deleteCookies("JSESSIONID") // Xóa sạch Cookie phiên làm việc
                        .permitAll()
                );


        return http.build();
    }

    // ... (Các Bean phía dưới GIỮ NGUYÊN: passwordEncoder, registeredClientRepository, jwkSource, etc.)

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // -----------------------------------------------------------
        // 1. Client Cũ (Dùng cho Swagger, Postman, Backend test)
        //    Vẫn giữ nguyên Secret để bảo mật
        // -----------------------------------------------------------
        RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("oidc-client")
                .clientSecret(passwordEncoder().encode("secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // Các Redirect URI cũ của bạn
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
                .redirectUri("http://localhost:8002/swagger-ui/oauth2-redirect.html")
                .redirectUri("http://192.168.80.1:8888/user-manager/swagger-ui/oauth2-redirect.html")
                .redirectUri("http://localhost:8888/user-manager/swagger-ui/oauth2-redirect.html")
                .redirectUri("http://localhost:8888/mall-service/swagger-ui/oauth2-redirect.html")
                .redirectUri("http://localhost:8888/notification-service/swagger-ui/oauth2-redirect.html")
                .postLogoutRedirectUri("http://127.0.0.1:8080/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .build();

        // -----------------------------------------------------------
        // 2. Client Mới (Dành riêng cho React Frontend)
        //    KHÔNG CÓ Secret, dùng PKCE
        // -----------------------------------------------------------
        RegisteredClient reactClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("react-client") // <-- Dùng ID này trong code React
                // KHÔNG set clientSecret ở đây

                // QUAN TRỌNG: Cho phép Public Client (không cần mật khẩu)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)

                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)

                // Redirect về React App (Port 3000)
                .redirectUri("http://localhost:5173")
                .redirectUri("http://localhost:5173/") // Thêm cái này cho chắc chắn tùy browser
                .postLogoutRedirectUri("http://localhost:5173")

                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)

                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true) // Hỏi user "Có đồng ý cho app React truy cập không?"
                        .requireProofKey(true) // <-- BẮT BUỘC: Bật PKCE để bảo mật cho React
                        .build())
                .build();

        // Trả về cả 2 client
        return new InMemoryRegisteredClientRepository(oidcClient, reactClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // ... (Giữ nguyên code cũ) ...
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    // Cấu hình DaoAuthenticationProvider để Spring biết dùng Service nào check pass
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService); // Sử dụng Service vừa tạo
        authProvider.setPasswordEncoder(passwordEncoder()); // Sử dụng BCrypt

        return authProvider;
    }

    // Expose AuthenticationManager ra thành Bean để AuthService dùng
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(UserRepository userRepository) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();
                if (principal != null && principal.getAuthorities() != null) {
                    // Lấy roles
                    Set<String> roles = principal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    context.getClaims().claim("roles", roles);

                    Object userPrincipal = principal.getPrincipal();

                    // Trường hợp login nội bộ
                    if (userPrincipal instanceof UserDetailsImpl) {
                        UserDetailsImpl userDetails = (UserDetailsImpl) userPrincipal;
                        context.getClaims().claim("userId", userDetails.getId());
                        context.getClaims().claim("email", userDetails.getEmail());
                    }

                    // Trường hợp login qua Google OAuth2
                    else if (userPrincipal instanceof DefaultOAuth2User) {
                        DefaultOAuth2User oauthUser = (DefaultOAuth2User) userPrincipal;
                        String email = (String) oauthUser.getAttributes().get("email");

                        // Tìm user trong DB theo email
                        User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found in DB"));

                        context.getClaims().claim("userId", user.getId());
                        context.getClaims().claim("email", user.getEmail());
                    }
                }
            }
        };
    }



}
