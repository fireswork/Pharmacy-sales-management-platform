package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.AddressRepository;
import com.drugstore.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // 获取当前用户的所有地址
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未登录或登录已过期"));
        }
        
        // 查询用户的所有地址
        List<Address> addresses = addressRepository.findByUserId(user.getId());
        
        // 转换为响应对象
        List<AddressResponse> addressResponses = addresses.stream()
            .map(address -> new AddressResponse(
                address.getId(),
                address.getReceiver(),
                address.getPhoneNumber(),
                address.getAddress(),
                address.isDefault()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(addressResponses, 200, "获取收货地址成功"));
    }
    
    // 添加新地址
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@RequestBody AddressRequest addressRequest) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未登录或登录已过期"));
        }
        
        // 创建新地址
        Address newAddress = new Address();
        newAddress.setUserId(user.getId());
        newAddress.setReceiver(addressRequest.getReceiver());
        newAddress.setPhoneNumber(addressRequest.getPhoneNumber());
        newAddress.setAddress(addressRequest.getAddress());
        newAddress.setDefault(addressRequest.isDefault());
        
        // 如果设为默认地址，需要将其他地址设为非默认
        if (addressRequest.isDefault()) {
            Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user.getId());
            if (defaultAddress != null) {
                defaultAddress.setDefault(false);
                addressRepository.save(defaultAddress);
            }
        }
        
        // 保存新地址
        Address savedAddress = addressRepository.save(newAddress);
        
        // 构建响应
        AddressResponse addressResponse = new AddressResponse(
            savedAddress.getId(),
            savedAddress.getReceiver(),
            savedAddress.getPhoneNumber(),
            savedAddress.getAddress(),
            savedAddress.isDefault()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(addressResponse, 201, "添加收货地址成功"));
    }
    
    // 更新地址
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @RequestBody AddressRequest addressRequest) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未登录或登录已过期"));
        }
        
        // 查找地址
        Address address = addressRepository.findById(id).orElse(null);
        
        if (address == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "地址不存在"));
        }
        
        // 验证地址是否属于当前用户
        if (!address.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(null, 403, "无权操作此地址"));
        }
        
        // 更新地址信息
        address.setReceiver(addressRequest.getReceiver());
        address.setPhoneNumber(addressRequest.getPhoneNumber());
        address.setAddress(addressRequest.getAddress());
        
        // 如果设为默认地址，需要将其他地址设为非默认
        if (addressRequest.isDefault() && !address.isDefault()) {
            Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user.getId());
            if (defaultAddress != null) {
                defaultAddress.setDefault(false);
                addressRepository.save(defaultAddress);
            }
            address.setDefault(true);
        }
        
        // 保存更新后的地址
        Address savedAddress = addressRepository.save(address);
        
        // 构建响应
        AddressResponse addressResponse = new AddressResponse(
            savedAddress.getId(),
            savedAddress.getReceiver(),
            savedAddress.getPhoneNumber(),
            savedAddress.getAddress(),
            savedAddress.isDefault()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(addressResponse, 200, "更新收货地址成功"));
    }
    
    // 删除地址
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未登录或登录已过期"));
        }
        
        // 查找地址
        Address address = addressRepository.findById(id).orElse(null);
        
        if (address == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "地址不存在"));
        }
        
        // 验证地址是否属于当前用户
        if (!address.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(null, 403, "无权操作此地址"));
        }
        
        // 删除地址
        addressRepository.delete(address);
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "删除收货地址成功"));
    }
    
    // 设置默认地址
    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(@PathVariable Long id) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未登录或登录已过期"));
        }
        
        // 查找地址
        Address address = addressRepository.findById(id).orElse(null);
        
        if (address == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "地址不存在"));
        }
        
        // 验证地址是否属于当前用户
        if (!address.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(null, 403, "无权操作此地址"));
        }
        
        // 将当前默认地址设为非默认
        Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user.getId());
        if (defaultAddress != null) {
            defaultAddress.setDefault(false);
            addressRepository.save(defaultAddress);
        }
        
        // 设置新的默认地址
        address.setDefault(true);
        Address savedAddress = addressRepository.save(address);
        
        // 构建响应
        AddressResponse addressResponse = new AddressResponse(
            savedAddress.getId(),
            savedAddress.getReceiver(),
            savedAddress.getPhoneNumber(),
            savedAddress.getAddress(),
            savedAddress.isDefault()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(addressResponse, 200, "设置默认地址成功"));
    }
} 