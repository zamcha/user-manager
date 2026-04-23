package vn.tqd.mobilemall.usermanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken implements java.io.Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Loại token: "REGISTER" (kích hoạt) hoặc "RESET_PASSWORD" (quên mk)
    @Column(nullable = false)
    private String type;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    // Hàm kiểm tra hết hạn
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}