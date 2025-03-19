package com.drugstore.api.repository;

import com.drugstore.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username); // 根据用户名查找用户
    
    List<User> findAll(); // 新增方法
} 