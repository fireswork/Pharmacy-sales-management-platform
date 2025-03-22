package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.model.request.InventoryAdjustRequest;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private StoreInventoryRepository inventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryRecordRepository inventoryRecordRepository;
    
    // 获取门店库存列表
    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getStoreInventory(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        Optional<Store> storeOpt = storeRepository.findById(storeId);
        if (storeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        List<StoreInventory> inventoryList = inventoryRepository.findByStoreId(storeId);
        
        // 如果有关键字，过滤结果
        if (keyword != null && !keyword.isEmpty()) {
            inventoryList = inventoryList.stream()
                .filter(inv -> inv.getProduct().getName().contains(keyword) || 
                               inv.getProduct().getCode().contains(keyword))
                .collect(Collectors.toList());
        }
        
        // 如果有分类，过滤结果
        if (category != null && !category.isEmpty()) {
            inventoryList = inventoryList.stream()
                .filter(inv -> {
                    Product product = inv.getProduct();
                    return product.getCategory() != null && 
                           category.equals(product.getCategory());
                })
                .collect(Collectors.toList());
        }
        
        // 根据库存状态过滤
        if (stockStatus != null && !stockStatus.isEmpty()) {
            inventoryList = inventoryList.stream()
                .filter(inv -> {
                    int quantity = inv.getQuantity();
                    
                    switch (stockStatus) {
                        case "normal":
                            return quantity > 30; // 正常库存
                        case "low":
                            return quantity > 0 && quantity <= 30; // 偏低
                        case "warning":
                            return quantity == 0; // 警告（无库存）
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
        }
        
        // 根据日期范围过滤（最近更新时间）
        if (startDate != null && endDate != null) {
            inventoryList = inventoryList.stream()
                .filter(inv -> {
                    Date updateTime = inv.getLastUpdateTime();
                    if (updateTime == null) return false;
                    
                    return !updateTime.before(startDate) && !updateTime.after(endDate);
                })
                .collect(Collectors.toList());
        }
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), inventoryList.size());
        List<StoreInventory> pageContent = start < end ? inventoryList.subList(start, end) : new ArrayList<>();
        
        // 转换为响应对象
        List<InventoryResponse> responseList = pageContent.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        Page<InventoryResponse> responsePage = new PageImpl<>(
            responseList,
            pageable,
            inventoryList.size()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(responsePage, 200, "获取门店库存成功"));
    }
    
    // 获取单个药品库存
    @GetMapping("/store/{storeId}/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getProductInventory(
            @PathVariable Long storeId,
            @PathVariable Long productId) {
        
        Optional<StoreInventory> inventoryOpt = inventoryRepository.findByStoreIdAndProductId(storeId, productId);
        if (inventoryOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "未找到该药品库存"));
        }
        
        InventoryResponse response = convertToResponse(inventoryOpt.get());
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "获取药品库存成功"));
    }
    
    // 获取当前登录用户门店的库存列表
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getCurrentStoreInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // 获取当前用户所属门店
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, 401, "未找到用户信息"));
            }
            
            Employee employee = employeeRepository.findByUser(user);
            if (employee == null || employee.getStore() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, 400, "当前用户未关联到门店"));
            }
            
            Long storeId = employee.getStore().getId();
            
            // 调用已有的获取门店库存的方法
            return getStoreInventory(storeId, page, size, keyword, category, stockStatus, startDate, endDate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "获取当前门店库存失败: " + e.getMessage()));
        }
    }
    
    // 转换为响应对象
    private InventoryResponse convertToResponse(StoreInventory inventory) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Product product = inventory.getProduct();
        
        // 获取分类名称
        String categoryName = "";
        if (product.getCategory() != null) {
            categoryName = product.getCategory().toString();
        }
        
        return new InventoryResponse(
            inventory.getId(),
            inventory.getStore().getId(),
            inventory.getStore().getName(),
            product.getId(),
            product.getName(),
            product.getCode(),
            categoryName,
            product.getSpecification(),
            product.getManufacturer(),
            product.getPrice(),
            product.getDescription(),
            product.getImage(),
            inventory.getQuantity(),
            inventory.getLastUpdateTime() != null ? dateFormat.format(inventory.getLastUpdateTime()) : ""
        );
    }

    // 更新库存数量和价格
    @PutMapping("/adjust")
    @Transactional
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustInventory(
            @RequestBody InventoryAdjustRequest request) {
        
        try {
            // 获取库存记录
            Long storeId = request.getStoreId();
            Long productId = request.getProductId();
            
            // 如果管理员未指定门店，获取当前用户所属门店
            if (storeId == null) {
                User currentUser = getCurrentUser();
                if (currentUser == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>(null, 401, "未授权操作"));
                }
                
                Employee employee = employeeRepository.findByUser(currentUser);
                if (employee == null || employee.getStore() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, 400, "当前用户未关联到门店"));
                }
                
                storeId = employee.getStore().getId();
            }
            
            // 检查产品和门店是否存在
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, 404, "产品不存在"));
            }
            
            Optional<Store> storeOpt = storeRepository.findById(storeId);
            if (storeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, 404, "门店不存在"));
            }
            
            Product product = productOpt.get();
            Store store = storeOpt.get();
            
            // 查找库存记录
            Optional<StoreInventory> inventoryOpt = inventoryRepository.findByStoreIdAndProductId(storeId, productId);
            StoreInventory inventory;
            
            if (inventoryOpt.isPresent()) {
                inventory = inventoryOpt.get();
            } else {
                // 如果不存在，创建新库存记录
                inventory = new StoreInventory();
                inventory.setStore(store);
                inventory.setProduct(product);
                inventory.setQuantity(0);
            }
            
            // 根据请求类型处理
            String adjustType = request.getAdjustType();
            if ("update".equals(adjustType)) {
                // 直接更新库存数量
                inventory.setQuantity(request.getQuantity());
                
                // 如果有价格参数，更新产品价格
                if (request.getPrice() != null) {
                    product.setPrice(request.getPrice());
                    productRepository.save(product);
                }
            } else if ("increase".equals(adjustType)) {
                // 增加库存
                inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
            } else if ("decrease".equals(adjustType)) {
                // 减少库存
                int newQuantity = inventory.getQuantity() - request.getQuantity();
                if (newQuantity < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(null, 400, "库存不足，当前库存: " + inventory.getQuantity()));
                }
                inventory.setQuantity(newQuantity);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, 400, "无效的调整类型"));
            }
            
            // 更新最后修改时间
            inventory.setLastUpdateTime(new Date());
            StoreInventory savedInventory = inventoryRepository.save(inventory);
            
            // 记录操作日志
            User currentUser = getCurrentUser();
            if (currentUser != null && !adjustType.equals("update")) {
                InventoryRecord record = new InventoryRecord();
                record.setStore(store);
                record.setProduct(product);
                record.setRecordType("increase".equals(adjustType) ? "inbound" : "outbound");
                record.setQuantity(request.getQuantity());
                record.setRecordTime(new Date());
                record.setSourceType("manual");
                record.setRemark(request.getRemark());
                record.setOperator(currentUser);
                // 保存记录
                inventoryRecordRepository.save(record);
            }
            
            return ResponseEntity.ok(new ApiResponse<>(convertToResponse(savedInventory), 200, "库存调整成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, 500, "库存调整失败: " + e.getMessage()));
        }
    }

    // 更新药品状态
    @PutMapping("/status")
    public ResponseEntity<ApiResponse<Void>> updateProductStatus(
            @RequestBody Map<String, Object> request) {
        
        try {
            Long storeId = ((Number) request.get("storeId")).longValue();
            Long productId = ((Number) request.get("productId")).longValue();
            String drugStatus = (String) request.get("drugStatus");
            
            // 检查产品和门店是否存在
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, 404, "产品不存在"));
            }
            
            Optional<Store> storeOpt = storeRepository.findById(storeId);
            if (storeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, 404, "门店不存在"));
            }
            
            // 更新产品状态
            Product product = productOpt.get();
            product.setStatus(drugStatus);
            productRepository.save(product);
            
            return ResponseEntity.ok(new ApiResponse<>(null, 200, "状态更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, 500, "状态更新失败: " + e.getMessage()));
        }
    }

    // 获取当前用户
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }
} 