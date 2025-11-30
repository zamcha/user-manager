package vn.tqd.mobilemall.usermanager.service;


import vn.tqd.mobilemall.usermanager.dto.request.CreateAccountRequest;
import vn.tqd.mobilemall.usermanager.dto.response.CreateAccountResponse;

public interface UserService {
    CreateAccountResponse createAccount(CreateAccountRequest request);
}