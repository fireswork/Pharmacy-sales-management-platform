package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.UserRepository;
import com.drugstore.api.repository.MemberRepository;
import com.drugstore.api.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        
        User user = userRepository.findByUsername(loginRequest.getUsername());

        if (user == null) {
            logger.warn("Login failed: User not found - {}", loginRequest.getUsername());
            ApiResponse<Object> response = new ApiResponse<>(null, 500, "用户名或密码不正确");
            return ResponseEntity.status(500).body(response);
        }
        
        boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        logger.info("Password match for user {}: {}", user.getUsername(), passwordMatches);
        
        if (passwordMatches) {
            // 检查会员状态
            if (user.getRole().equals("user") && user.getMember() != null) {
                if (!"正常".equals(user.getMember().getStatus())) {
                    logger.warn("Login failed: User account is disabled - {}", loginRequest.getUsername());
                    ApiResponse<Object> response = new ApiResponse<>(null, 500, "账号已被停用，请联系管理员");
                    return ResponseEntity.status(500).body(response);
                }
            }
            
            String token = jwtTokenUtil.generateToken(user.getUsername());
            Map<String, String> data = new HashMap<>();
            data.put("access_token", token);
            
            logger.info("Login successful for user: {}", user.getUsername());
            ApiResponse<Object> response = new ApiResponse<>(data, 200, "Login successful");
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Login failed: Invalid password for user - {}", loginRequest.getUsername());
            ApiResponse<Object> response = new ApiResponse<>(null, 500, "用户名或密码不正确");
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserInfoResponse>> register(@RequestBody RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(registerRequest.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "用户名已存在"));
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole("user");
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 创建会员信息
        Member member = new Member();
        member.setUser(savedUser);
        member.setName(registerRequest.getUsername()); // 默认使用用户名作为会员姓名
        member.setStatus("正常");
        member.setPoints(0);
        member.setTotalSpending(0.0);
        member.setMemberLevel("bronze");
        
        // 生成会员ID
        long count = memberRepository.count();
        String memberId = String.format("M%03d", count + 1);
        member.setMemberId(memberId);
        
        // 保存会员信息
        Member savedMember = memberRepository.save(member);
        
        // 构建响应
        MemberInfoResponse memberInfo = new MemberInfoResponse(
            savedMember.getMemberId(),
            savedMember.getName(),
            savedMember.getPhoneNumber() != null ? savedMember.getPhoneNumber() : "",
            savedMember.getEmail() != null ? savedMember.getEmail() : "",
            savedMember.getBirthday() != null ? savedMember.getBirthday().toString() : "",
            savedMember.getGender() != null ? savedMember.getGender() : "",
            savedMember.getMemberLevel(),
            savedMember.getPoints(),
            savedMember.getTotalSpending(),
            savedMember.getRegistrationTime() != null ? savedMember.getRegistrationTime().toString() : "",
            savedMember.getStatus()
        );
        
        UserInfoResponse userInfo = new UserInfoResponse(
            savedUser.getUsername(),
            savedUser.getRole(),
            memberInfo
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(userInfo, 200, "注册成功"));
    }
} 