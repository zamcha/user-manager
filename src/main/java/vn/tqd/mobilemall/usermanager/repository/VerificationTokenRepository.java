package vn.tqd.mobilemall.usermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.tqd.mobilemall.usermanager.entity.VerificationToken;
import vn.tqd.mobilemall.usermanager.entity.User;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserAndType(User user, String type);
    void deleteByToken(String token);
}