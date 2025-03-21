package com.drugstore.api.config;

import com.drugstore.api.model.User;
import com.drugstore.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 检查是否已存在管理员账号
        if (!userRepository.existsByUsername("admin")) {
            // 创建管理员账号
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456")); // 默认密码：123456
            admin.setRole("ADMIN");
            admin.setName("系统管理员");
            admin.setStatus("active");
            
            userRepository.save(admin);
            
            System.out.println("系统初始化: 已创建管理员账号 (用户名: admin, 密码: 123456)");
        }
    }
} 