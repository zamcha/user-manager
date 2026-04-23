package vn.tqd.mobilemall.usermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Tìm Role dựa trên Enum (Ví dụ: ERole.ROLE_USER)
    Optional<Role> findByName(ERole name);
}