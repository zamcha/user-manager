package vn.tqd.mobilemall.usermanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import vn.tqd.mobilemall.usermanager.dto.request.RegisterRequest;
import vn.tqd.mobilemall.usermanager.dto.request.UpdateProfileRequest;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.Role;
import vn.tqd.mobilemall.usermanager.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // 1. Map từ RegisterRequest -> Entity User
    // ✅ QUAN TRỌNG: Thêm dòng ignore = true cho roles
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    User toUser(RegisterRequest request);

    // 2. Map từ Entity -> Response (Giữ nguyên)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserResponse toUserResponse(User user);

    // 3. Update Profile (Giữ nguyên)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true) // Update profile không cho sửa quyền
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    void updateUserFromRequest(UpdateProfileRequest request, @MappingTarget User user);

    // Helper method: Set<Role> -> Set<String>
    @Named("mapRoles")
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}