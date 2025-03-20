package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.EmployeeRepository;
import com.drugstore.api.repository.StoreRepository;
import com.drugstore.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.drugstore.api.model.ApiResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // 获取所有员工（带分页、搜索、状态和门店筛选）
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeInfoResponse>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long storeId) {
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Employee> employeePage;
        
        // 根据不同的筛选条件查询
        if (storeId != null) {
            // 按门店筛选
            if (keyword != null && !keyword.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
                // 同时按门店、关键字和状态筛选
                employeePage = employeeRepository.findByStoreIdAndStatusAndNameContainingOrPhoneNumberContaining(
                    storeId, status, keyword, keyword, pageable);
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                // 按门店和关键字筛选
                employeePage = employeeRepository.findByStoreIdAndNameContainingOrPhoneNumberContaining(
                    storeId, keyword, keyword, pageable);
            } else if (status != null && !status.trim().isEmpty()) {
                // 按门店和状态筛选
                employeePage = employeeRepository.findByStoreIdAndStatus(storeId, status, pageable);
            } else {
                // 只按门店筛选
                employeePage = employeeRepository.findByStoreId(storeId, pageable);
            }
        } else {
            // 不按门店筛选
            if (keyword != null && !keyword.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
                // 同时按关键字和状态筛选
                employeePage = employeeRepository.findByStatusAndNameContainingOrPhoneNumberContaining(
                    status, keyword, keyword, pageable);
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                // 只按关键字筛选
                employeePage = employeeRepository.findByNameContainingOrPhoneNumberContaining(
                    keyword, keyword, pageable);
            } else if (status != null && !status.trim().isEmpty()) {
                // 只按状态筛选
                employeePage = employeeRepository.findByStatus(status, pageable);
            } else {
                // 不筛选，查询所有员工
                employeePage = employeeRepository.findAll(pageable);
            }
        }
        
        // 转换为 EmployeeInfoResponse 分页
        List<EmployeeInfoResponse> employeeInfoList = new ArrayList<>();
        
        for (Employee employee : employeePage.getContent()) {
            StoreInfoResponse storeInfo = null;
            if (employee.getStore() != null) {
                Store store = employee.getStore();
                storeInfo = new StoreInfoResponse(
                    store.getId(),
                    store.getCode(),
                    store.getName(),
                    store.getAddress() != null ? store.getAddress() : "",
                    store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
                    store.getOpenTime() != null ? store.getOpenTime() : "",
                    store.getCloseTime() != null ? store.getCloseTime() : "",
                    store.getStatus()
                );
            }
            
            EmployeeInfoResponse employeeInfo = new EmployeeInfoResponse(
                employee.getEmployeeId(),
                employee.getName(),
                employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "",
                employee.getEmail() != null ? employee.getEmail() : "",
                storeInfo,
                employee.getHireDate() != null ? employee.getHireDate().toString() : "",
                employee.getStatus()
            );
            
            employeeInfoList.add(employeeInfo);
        }
        
        Page<EmployeeInfoResponse> employeeInfoPage = new PageImpl<>(
            employeeInfoList, 
            pageable, 
            employeePage.getTotalElements()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(employeeInfoPage, 200, "获取员工列表成功"));
    }

    // 获取所有门店（只返回营业中的门店）
    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<List<StoreInfoResponse>>> getAllStores() {
        List<Store> stores = storeRepository.findByStatus("active");
        
        List<StoreInfoResponse> storeInfoList = stores.stream()
            .map(store -> new StoreInfoResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getAddress() != null ? store.getAddress() : "",
                store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
                store.getOpenTime() != null ? store.getOpenTime() : "",
                store.getCloseTime() != null ? store.getCloseTime() : "",
                store.getStatus()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(storeInfoList, 200, "获取门店列表成功"));
    }

    // 添加员工
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeInfoResponse>> addEmployee(@RequestBody EmployeeRequest employeeRequest) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(employeeRequest.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "用户名已存在"));
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(employeeRequest.getUsername());
        // 默认密码为 123456
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("employee");
        
        User savedUser = userRepository.save(user);
        
        // 创建员工
        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setName(employeeRequest.getName());
        employee.setPhoneNumber(employeeRequest.getPhoneNumber());
        employee.setEmail(employeeRequest.getEmail());
        employee.setHireDate(employeeRequest.getHireDate() != null ? employeeRequest.getHireDate() : new Date());
        employee.setStatus("在职");
        
        // 设置门店
        if (employeeRequest.getStoreId() != null) {
            Store store = storeRepository.findById(employeeRequest.getStoreId()).orElse(null);
            if (store != null) {
                employee.setStore(store);
            }
        }
        
        // 生成员工ID
        long count = employeeRepository.countEmployees();
        String employeeId = String.format("EMP%03d", count + 1);
        employee.setEmployeeId(employeeId);
        
        // 保存员工信息
        Employee savedEmployee = employeeRepository.save(employee);
        
        // 构建响应
        StoreInfoResponse storeInfo = null;
        if (savedEmployee.getStore() != null) {
            Store store = savedEmployee.getStore();
            storeInfo = new StoreInfoResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getAddress() != null ? store.getAddress() : "",
                store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
                store.getOpenTime() != null ? store.getOpenTime() : "",
                store.getCloseTime() != null ? store.getCloseTime() : "",
                store.getStatus()
            );
        }
        
        EmployeeInfoResponse employeeInfo = new EmployeeInfoResponse(
            savedEmployee.getEmployeeId(),
            savedEmployee.getName(),
            savedEmployee.getPhoneNumber() != null ? savedEmployee.getPhoneNumber() : "",
            savedEmployee.getEmail() != null ? savedEmployee.getEmail() : "",
            storeInfo,
            savedEmployee.getHireDate() != null ? savedEmployee.getHireDate().toString() : "",
            savedEmployee.getStatus()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(employeeInfo, 201, "员工添加成功"));
    }

    // 更新员工状态
    @PutMapping("/{employeeId}/status")
    public ResponseEntity<ApiResponse<EmployeeInfoResponse>> updateEmployeeStatus(
            @PathVariable String employeeId,
            @RequestParam String status) {
        
        // 查找员工
        Employee employee = employeeRepository.findByEmployeeId(employeeId);
        
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "员工不存在"));
        }
        
        // 更新状态
        employee.setStatus(status);
        Employee savedEmployee = employeeRepository.save(employee);
        
        // 构建响应
        StoreInfoResponse storeInfo = null;
        if (savedEmployee.getStore() != null) {
            Store store = savedEmployee.getStore();
            storeInfo = new StoreInfoResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getAddress() != null ? store.getAddress() : "",
                store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
                store.getOpenTime() != null ? store.getOpenTime() : "",
                store.getCloseTime() != null ? store.getCloseTime() : "",
                store.getStatus()
            );
        }
        
        EmployeeInfoResponse employeeInfo = new EmployeeInfoResponse(
            savedEmployee.getEmployeeId(),
            savedEmployee.getName(),
            savedEmployee.getPhoneNumber() != null ? savedEmployee.getPhoneNumber() : "",
            savedEmployee.getEmail() != null ? savedEmployee.getEmail() : "",
            storeInfo,
            savedEmployee.getHireDate() != null ? savedEmployee.getHireDate().toString() : "",
            savedEmployee.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(employeeInfo, 200, "员工状态更新成功"));
    }

    // 重置员工密码
    @PutMapping("/{employeeId}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetEmployeePassword(@PathVariable String employeeId) {
        // 查找员工
        Employee employee = employeeRepository.findByEmployeeId(employeeId);
        
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "员工不存在"));
        }
        
        // 获取关联的用户
        User user = employee.getUser();
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "用户不存在"));
        }
        
        // 重置密码为 123456
        user.setPassword(passwordEncoder.encode("123456"));
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "密码重置成功"));
    }

    // 编辑员工
    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeInfoResponse>> updateEmployee(
            @PathVariable String employeeId,
            @RequestBody EmployeeRequest employeeRequest) {
        
        // 查找员工
        Employee employee = employeeRepository.findByEmployeeId(employeeId);
        
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "员工不存在"));
        }
        
        // 更新员工信息
        if (employeeRequest.getName() != null) {
            employee.setName(employeeRequest.getName());
        }
        if (employeeRequest.getPhoneNumber() != null) {
            employee.setPhoneNumber(employeeRequest.getPhoneNumber());
        }
        if (employeeRequest.getEmail() != null) {
            employee.setEmail(employeeRequest.getEmail());
        }
        if (employeeRequest.getHireDate() != null) {
            employee.setHireDate(employeeRequest.getHireDate());
        }
        
        // 更新门店
        if (employeeRequest.getStoreId() != null) {
            Store store = storeRepository.findById(employeeRequest.getStoreId()).orElse(null);
            if (store != null) {
                employee.setStore(store);
            }
        }
        
        // 保存员工信息
        Employee savedEmployee = employeeRepository.save(employee);
        
        // 构建响应
        StoreInfoResponse storeInfo = null;
        if (savedEmployee.getStore() != null) {
            Store store = savedEmployee.getStore();
            storeInfo = new StoreInfoResponse(
                store.getId(),
                store.getCode(),
                store.getName(),
                store.getAddress() != null ? store.getAddress() : "",
                store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
                store.getOpenTime() != null ? store.getOpenTime() : "",
                store.getCloseTime() != null ? store.getCloseTime() : "",
                store.getStatus()
            );
        }
        
        EmployeeInfoResponse employeeInfo = new EmployeeInfoResponse(
            savedEmployee.getEmployeeId(),
            savedEmployee.getName(),
            savedEmployee.getPhoneNumber() != null ? savedEmployee.getPhoneNumber() : "",
            savedEmployee.getEmail() != null ? savedEmployee.getEmail() : "",
            storeInfo,
            savedEmployee.getHireDate() != null ? savedEmployee.getHireDate().toString() : "",
            savedEmployee.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(employeeInfo, 200, "员工信息更新成功"));
    }
} 