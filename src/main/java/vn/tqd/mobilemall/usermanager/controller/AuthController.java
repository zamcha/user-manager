package vn.tqd.mobilemall.usermanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.tqd.mobilemall.usermanager.dto.request.LoginRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.response.JWTResponse;
import vn.tqd.mobilemall.usermanager.service.AuthService;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * API Đăng ký tài khoản (Local)
     * URL: POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return ResponseEntity.ok("Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
    }

    /**
     * API Đăng nhập (Local)
     * URL: POST /api/v1/auth/login
     */
//    @PostMapping("/login")
//    public ResponseEntity<JWTResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
//        // AuthService sẽ trả về JwtResponse chứa Token + Info
//        JWTResponse jwtResponse = authService.login(loginRequest);
//        return ResponseEntity.ok(jwtResponse);
//    }
//    @PostMapping("/logout")
//    @Operation(summary = "Đăng xuất tài khoản", description = "Xóa context xác thực. Client cần tự xóa token ở LocalStorage.")
//    public ResponseEntity<?> logout() {
//        return ResponseEntity.ok().build();
//    }

}