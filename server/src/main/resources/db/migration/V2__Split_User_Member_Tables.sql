-- 创建会员表
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    birthday DATE,
    gender VARCHAR(10),
    member_level VARCHAR(20),
    points INT,
    total_spending DOUBLE,
    registration_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20),
    user_id BIGINT UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 迁移现有用户数据到会员表
INSERT INTO members (
    member_id, 
    name, 
    phone_number, 
    email, 
    birthday, 
    gender, 
    member_level, 
    points, 
    total_spending, 
    registration_time, 
    status, 
    user_id
)
SELECT 
    member_id, 
    username, 
    phone_number, 
    email, 
    birthday, 
    gender, 
    member_level, 
    points, 
    total_spending, 
    registration_time, 
    status, 
    id
FROM users
WHERE role = 'user';

-- 修改用户表，删除迁移到会员表的字段
ALTER TABLE users
    DROP COLUMN IF EXISTS member_id,
    DROP COLUMN IF EXISTS phone_number,
    DROP COLUMN IF EXISTS email,
    DROP COLUMN IF EXISTS birthday,
    DROP COLUMN IF EXISTS gender,
    DROP COLUMN IF EXISTS member_level,
    DROP COLUMN IF EXISTS points,
    DROP COLUMN IF EXISTS total_spending,
    DROP COLUMN IF EXISTS registration_time,
    DROP COLUMN IF EXISTS status; 