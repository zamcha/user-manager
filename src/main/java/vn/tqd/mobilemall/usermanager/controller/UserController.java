package vn.tqd.mobilemall.usermanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.tqd.mobilemall.common.api.response.ApiResponse;
import vn.tqd.mobilemall.usermanager.dto.request.CreateAccountRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;
import vn.tqd.mobilemall.usermanager.service.UserService;

@RestController
@RequestMapping(value = "/api/v1/user/accounts")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateAccountResponse>> createAccount(
            @RequestBody CreateAccountRequest request
    ){
        CreateAccountResponse response = userService.createAccount(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<String>> test(){
        return ResponseEntity.ok(ApiResponse.success("Hello world"));
    }
}
