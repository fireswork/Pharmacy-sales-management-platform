package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.UserRepository;
import com.drugstore.api.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.drugstore.api.model.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 获取所有会员（带分页、搜索和状态筛选）
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MemberInfoResponse>>> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Member> memberPage;
        
        // 根据不同的筛选条件查询
        if (keyword != null && !keyword.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
            // 同时按关键字和状态筛选
            memberPage = memberRepository.findByStatusAndNameContainingOrPhoneNumberContaining(
                status, keyword, keyword, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 只按关键字筛选
            memberPage = memberRepository.findByNameContainingOrPhoneNumberContaining(
                keyword, keyword, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            // 只按状态筛选
            memberPage = memberRepository.findByStatus(status, pageable);
        } else {
            // 不筛选，查询所有会员
            memberPage = memberRepository.findAll(pageable);
        }
        
        // 直接转换为 MemberInfoResponse 分页
        Page<MemberInfoResponse> memberInfoPage = memberPage.map(member -> {
            User user = member.getUser();
            
            return new MemberInfoResponse(
                member.getMemberId(),
                member.getName(),
                member.getPhoneNumber() != null ? member.getPhoneNumber() : "",
                member.getEmail() != null ? member.getEmail() : "",
                member.getBirthday() != null ? member.getBirthday().toString() : "",
                member.getGender() != null ? member.getGender() : "",
                member.getMemberLevel(),
                member.getPoints(),
                member.getTotalSpending(),
                member.getRegistrationTime() != null ? member.getRegistrationTime().toString() : "",
                member.getStatus()
            );
        });
        
        return ResponseEntity.ok(new ApiResponse<>(memberInfoPage, 200, "获取会员列表成功"));
    }

    // 新增会员
    @PostMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> addMember(@RequestBody MemberRequest memberRequest) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(memberRequest.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "用户名已存在"));
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(memberRequest.getUsername());
        user.setPassword(passwordEncoder.encode("12345678")); // 默认密码
        user.setRole("user");
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 创建会员信息
        Member member = new Member();
        member.setUser(savedUser);
        member.setName(memberRequest.getName() != null ? memberRequest.getName() : memberRequest.getUsername());
        member.setPhoneNumber(memberRequest.getPhoneNumber());
        member.setEmail(memberRequest.getEmail());
        member.setBirthday(memberRequest.getBirthday());
        member.setGender(memberRequest.getGender());
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
            .body(new ApiResponse<>(userInfo, 201, "会员添加成功"));
    }

    // 更新会员状态
    @PutMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberStatus(
            @PathVariable String memberId,
            @RequestParam String status) {
        
        // 查找会员
        Member member = memberRepository.findByMemberId(memberId);
        
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "会员不存在"));
        }
        
        // 更新状态
        member.setStatus(status);
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
        
        return ResponseEntity.ok(new ApiResponse<>(memberInfo, 200, "会员状态更新成功"));
    }

    // 删除会员
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String memberId) {
        // 查找会员
        Member member = memberRepository.findByMemberId(memberId);
        
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "会员不存在"));
        }
        
        // 获取关联的用户
        User user = member.getUser();
        
        // 删除会员和用户
        memberRepository.delete(member);
        if (user != null) {
            userRepository.delete(user);
        }
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "会员删除成功"));
    }

    // 获取当前用户的会员信息
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getCurrentMember(Authentication authentication) {
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
        
        // 获取会员信息
        Member member = memberRepository.findByUser(user);
        
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "会员信息不存在"));
        }
        
        // 构建会员信息响应
        MemberInfoResponse memberInfo = new MemberInfoResponse(
            member.getMemberId(),
            member.getName(),
            member.getPhoneNumber() != null ? member.getPhoneNumber() : "",
            member.getEmail() != null ? member.getEmail() : "",
            member.getBirthday() != null ? member.getBirthday().toString() : "",
            member.getGender() != null ? member.getGender() : "",
            member.getMemberLevel(),
            member.getPoints(),
            member.getTotalSpending(),
            member.getRegistrationTime() != null ? member.getRegistrationTime().toString() : "",
            member.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(memberInfo, 200, "获取会员信息成功"));
    }

    // 更新当前用户的会员信息
    @PutMapping("/current")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateCurrentMember(
            Authentication authentication,
            @RequestBody UserUpdateRequest updateRequest) {
        
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
        
        // 获取会员信息
        Member member = memberRepository.findByUser(user);
        
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "会员信息不存在"));
        }
        
        // 更新会员信息
        if (updateRequest.getName() != null) {
            member.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            member.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getEmail() != null) {
            member.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getBirthday() != null) {
            member.setBirthday(updateRequest.getBirthday());
        }
        if (updateRequest.getGender() != null) {
            member.setGender(updateRequest.getGender());
        }
        
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
        
        return ResponseEntity.ok(new ApiResponse<>(memberInfo, 200, "会员信息更新成功"));
    }

    // 编辑会员信息
    @PutMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMember(
            @PathVariable String memberId,
            @RequestBody MemberUpdateRequest updateRequest) {
        
        // 查找会员
        Member member = memberRepository.findByMemberId(memberId);
        
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "会员不存在"));
        }
        
        // 更新会员信息
        if (updateRequest.getName() != null) {
            member.setName(updateRequest.getName());
        }
        if (updateRequest.getPhoneNumber() != null) {
            member.setPhoneNumber(updateRequest.getPhoneNumber());
        }
        if (updateRequest.getEmail() != null) {
            member.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getBirthday() != null) {
            member.setBirthday(updateRequest.getBirthday());
        }
        if (updateRequest.getGender() != null) {
            member.setGender(updateRequest.getGender());
        }
        
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
        
        return ResponseEntity.ok(new ApiResponse<>(memberInfo, 200, "会员信息更新成功"));
    }
} 