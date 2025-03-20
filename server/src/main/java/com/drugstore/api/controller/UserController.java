package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.drugstore.api.model.ApiResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 获取当前用户基本信息
    @GetMapping
    public ResponseEntity<ApiResponse<UserBasicInfoResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未授权，请先登录"));
        }
        
        // 从认证对象中获取用户名
        String username = authentication.getName();
        
        // 从数据库中查询用户
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "用户不存在"));
        }
        
        // 构建基本用户信息响应
        UserBasicInfoResponse userInfo = new UserBasicInfoResponse(
            user.getUsername(),
            user.getRole()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(userInfo, 200, "获取用户信息成功"));
    }

    // 修改密码
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> updatePassword(
            Authentication authentication,
            @RequestBody PasswordUpdateRequest passwordRequest) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未授权，请先登录"));
        }
        
        // 从认证对象中获取用户名
        String username = authentication.getName();
        
        // 从数据库中查询用户
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "用户不存在"));
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(passwordRequest.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "旧密码不正确"));
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
        userRepository.save(user);
        
        // 返回需要退出登录的标志
        Map<String, Boolean> result = new HashMap<>();
        result.put("requireLogout", true);
        
        return ResponseEntity.ok(new ApiResponse<>(result, 200, "密码修改成功，请使用新密码重新登录"));
    }
} 