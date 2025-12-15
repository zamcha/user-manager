package vn.tqd.mobilemall.usermanager.service;

import vn.tqd.mobilemall.usermanager.dto.request.LoginRequest;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.response.JWTResponse;
import vn.tqd.mobilemall.usermanager.entity.User;

public interface AuthService {
    void registerUser(RegisterRequest request);

    JWTResponse login(LoginRequest request);

}
