-- 1. Bảng USERS (Bỏ cột version, giữ lại Index để chịu tải cao)
CREATE TABLE users (
                       id CHAR(36) NOT NULL,

                       email VARCHAR(150) NOT NULL,
                       phone_number VARCHAR(15) DEFAULT NULL,
                       password VARCHAR(255) DEFAULT NULL,

                       full_name VARCHAR(100) DEFAULT NULL,
                       avatar_url VARCHAR(500) DEFAULT NULL,

                       provider_id VARCHAR(100) DEFAULT NULL, -- Google ID
                       auth_provider VARCHAR(20) DEFAULT 'LOCAL',

                       is_active BOOLEAN DEFAULT TRUE,
                       is_verified BOOLEAN DEFAULT FALSE,

                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP DEFAULT NULL,

                       PRIMARY KEY (id),

    -- Indexing tối ưu tốc độ login/search
                       UNIQUE INDEX idx_users_email (email),
                       UNIQUE INDEX idx_users_phone (phone_number),
                       INDEX idx_users_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng ROLES
CREATE TABLE roles (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(20) NOT NULL UNIQUE,
                       description VARCHAR(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng USER_ROLES (Trung gian)
CREATE TABLE user_roles (
                            user_id CHAR(36) NOT NULL,
                            role_id INT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. INSERT SẴN DATA QUYỀN (Chạy 1 lần)
INSERT INTO roles (name, description) VALUES
                                          ('ROLE_USER', 'Khách hàng mua sắm'),
                                          ('ROLE_MANAGER', 'Quản lý kho và đơn hàng'),
                                          ('ROLE_ADMIN', 'Quản trị hệ thống cao nhất');
-- 5. Bảng VERIFICATION_TOKENS (Lưu token kích hoạt & quên mật khẩu)
CREATE TABLE verification_tokens (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                     token VARCHAR(255) NOT NULL,
                                     type VARCHAR(50) NOT NULL, -- 'REGISTER' hoặc 'RESET_PASSWORD'
                                     expiry_date TIMESTAMP NOT NULL,

                                     user_id CHAR(36) NOT NULL,

    -- Index để tìm token cho nhanh
                                     UNIQUE INDEX idx_verify_token (token),

    -- Khóa ngoại liên kết với bảng users
                                     CONSTRAINT fk_verify_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;