package vn.tqd.mobilemall.usermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.tqd.mobilemall.usermanager.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

