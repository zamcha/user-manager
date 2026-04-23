package vn.tqd.mobilemall.usermanager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tqd.mobilemall.usermanager.dto.response.UserResponse;
import vn.tqd.mobilemall.usermanager.entity.ERole;
import vn.tqd.mobilemall.usermanager.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // 1. Dùng cho Login: Tìm user bằng email
    // Trả về Optional để tránh lỗi NullPointerException
    Optional<User> findByEmail(String email);

    Optional<User> findById(String id);

    // 2. Dùng cho Đăng ký: Kiểm tra email đã tồn tại chưa
    Boolean existsByEmail(String email);

    // 3. (Optional) Kiểm tra số điện thoại đã tồn tại chưa
    Boolean existsByPhoneNumber(String phoneNumber);

    // 4. (Optional) Tìm user theo Verification Code (nếu làm chức năng gửi mail kích hoạt)
    // Optional<User> findByVerificationCode(String code);

    // 5. Tìm theo Google ID (Nếu bạn muốn tìm chính xác theo Provider ID)
    Optional<User> findByProviderId(String providerId);
    /**
     * Tìm kiếm User nâng cao kết hợp Phân trang.
     * Logic:
     * - Nếu keyword NULL hoặc RỖNG ('') -> Lấy tất cả user.
     * - Nếu có keyword -> Tìm xem nó có xuất hiện trong Email, Tên hoặc SĐT không.
     * - LOWER(...): Chuyển về chữ thường để tìm không phân biệt hoa thường (VD: gõ "tung" tìm ra "Tung").
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.roles r " +
            "WHERE " +
            "(:roleName IS NULL OR :roleName = '' OR r.name = :roleName) " +
            "AND " +
            // 2. Cụm điều kiện Keyword
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " u.phoneNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(
            @Param("roleName") ERole roleName,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}