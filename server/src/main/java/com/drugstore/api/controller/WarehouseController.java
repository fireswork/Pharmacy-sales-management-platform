package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.model.request.InventoryAdjustRequest;
import com.drugstore.api.model.response.InventoryRecordResponse;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private InventoryRecordRepository inventoryRecordRepository;
    
    @Autowired
    private StoreInventoryRepository storeInventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    // 添加获取当前用户的方法
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }
    
    // 添加通过用户获取员工信息的方法
    private Employee getEmployeeByUser(User user) {
        if (user == null) return null;
        return employeeRepository.findByUser(user);
    }
    
    // 添加转换方法，将InventoryRecord转换为InventoryRecordResponse
    private InventoryRecordResponse convertToResponse(InventoryRecord record) {
        InventoryRecordResponse response = new InventoryRecordResponse();
        response.setId(record.getId());
        response.setRecordTime(record.getRecordTime());
        response.setRecordType(record.getRecordType());
        response.setQuantity(record.getQuantity());
        response.setSourceType(record.getSourceType());
        response.setSourceId(record.getSourceId());
        response.setBatchNumber(record.getBatchNumber());
        response.setProductionDate(record.getProductionDate());
        response.setExpirationDate(record.getExpirationDate());
        response.setRemark(record.getRemark());
        
        // 设置产品信息
        if (record.getProduct() != null) {
            response.setProductId(record.getProduct().getId());
            response.setProductName(record.getProduct().getName());
            response.setProductCode(record.getProduct().getCode());
        }
        
        // 设置门店信息
        if (record.getStore() != null) {
            response.setStoreId(record.getStore().getId());
            response.setStoreName(record.getStore().getName());
        }
        
        // 设置操作人信息
        if (record.getOperator() != null) {
            response.setOperatorName(record.getOperator().getUsername());
        }
        
        return response;
    }
    
    // 获取库存记录列表
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Page<InventoryRecordResponse>>> getInventoryRecords(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        // 如果没有指定门店，则使用当前用户所在门店
        if (storeId == null) {
            User currentUser = getCurrentUser();
            Employee employee = getEmployeeByUser(currentUser);
            if (employee != null && employee.getStore() != null) {
                storeId = employee.getStore().getId();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, 400, "请指定门店ID或确保当前用户关联了门店"));
            }
        }
        
        // 检查门店是否存在
        Optional<Store> storeOpt = storeRepository.findById(storeId);
        if (storeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        // 处理日期范围
        if (startDate != null && endDate == null) {
            // 如果只有开始日期，则将结束日期设置为当天结束
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            endDate = calendar.getTime();
        } else if (startDate == null && endDate != null) {
            // 如果只有结束日期，则将开始日期设置为一个月前
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(endDate);
            calendar.add(Calendar.MONTH, -1);
            startDate = calendar.getTime();
        } else if (startDate != null && endDate != null) {
            // 如果都有，调整开始日期为当天开始，结束日期为当天结束
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startDate = startCal.getTime();
            
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endDate);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);
            endDate = endCal.getTime();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryRecord> recordPage;
        
        // 根据不同条件组合查询
        if (recordType != null && keyword != null && startDate != null && endDate != null) {
            // 按类型、关键字和时间范围查询
            recordPage = inventoryRecordRepository.findByStoreIdAndRecordTypeAndKeywordAndRecordTimeBetween(
                storeId, recordType, keyword, startDate, endDate, pageable);
        } else if (recordType != null && keyword != null) {
            // 按类型和关键字查询
            recordPage = inventoryRecordRepository.findByStoreIdAndRecordTypeAndKeyword(
                storeId, recordType, keyword, pageable);
        } else if (recordType != null && startDate != null && endDate != null) {
            // 按类型和时间范围查询
            recordPage = inventoryRecordRepository.findByStoreIdAndRecordTypeAndRecordTimeBetween(
                storeId, recordType, startDate, endDate, pageable);
        } else if (keyword != null && startDate != null && endDate != null) {
            // 按关键字和时间范围查询
            recordPage = inventoryRecordRepository.findByStoreIdAndKeywordAndRecordTimeBetween(
                storeId, keyword, startDate, endDate, pageable);
        } else if (recordType != null) {
            // 只按类型查询
            recordPage = inventoryRecordRepository.findByStoreIdAndRecordType(
                storeId, recordType, pageable);
        } else if (keyword != null) {
            // 只按关键字查询
            recordPage = inventoryRecordRepository.findByStoreIdAndKeyword(
                storeId, keyword, pageable);
        } else if (startDate != null && endDate != null) {
            // 只按时间范围查询
            recordPage = inventoryRecordRepository.findByStoreIdAndRecordTimeBetween(
                storeId, startDate, endDate, pageable);
        } else {
            // 查询所有记录
            recordPage = inventoryRecordRepository.findByStoreId(storeId, pageable);
        }
        
        // 转换为响应对象
        List<InventoryRecordResponse> responseList = recordPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        Page<InventoryRecordResponse> responsePage = new PageImpl<>(
            responseList,
            pageable,
            recordPage.getTotalElements()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(responsePage, 200, "获取库存记录成功"));
    }
    
    // 获取产品的库存记录列表
    @GetMapping("/records/product/{productId}")
    public ResponseEntity<ApiResponse<Page<InventoryRecordResponse>>> getProductInventoryRecords(
            @PathVariable Long productId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String recordType) {
        
        // 检查产品是否存在
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "产品不存在"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        List<InventoryRecord> records;
        
        // 如果指定了门店，则查询指定门店的产品记录
        if (storeId != null) {
            records = inventoryRecordRepository.findByStoreIdAndProductId(storeId, productId);
        } else {
            // 如果未指定门店，则查询所有门店的该产品记录
            records = inventoryRecordRepository.findAll().stream()
                .filter(record -> record.getProduct().getId().equals(productId))
                .collect(Collectors.toList());
        }
        
        // 如果指定了记录类型，进一步过滤
        if (recordType != null && !recordType.isEmpty()) {
            records = records.stream()
                .filter(record -> recordType.equals(record.getRecordType()))
                .collect(Collectors.toList());
        }
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), records.size());
        List<InventoryRecord> pageContent = start < end ? records.subList(start, end) : new ArrayList<>();
        
        // 转换为响应对象
        List<InventoryRecordResponse> responseList = pageContent.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        Page<InventoryRecordResponse> responsePage = new PageImpl<>(
            responseList,
            pageable,
            records.size()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(responsePage, 200, "获取产品库存记录成功"));
    }
    
    // 手动调整库存
    @PostMapping("/adjust")
    @Transactional
    public ResponseEntity<ApiResponse<InventoryRecordResponse>> adjustInventory(
            @RequestBody InventoryAdjustRequest request) {
        
        // 检查门店和产品是否存在
        Optional<Store> storeOpt = storeRepository.findById(request.getStoreId());
        if (storeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        Optional<Product> productOpt = productRepository.findById(request.getProductId());
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "产品不存在"));
        }
        
        // 获取当前用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(null, 401, "未授权操作"));
        }
        
        Store store = storeOpt.get();
        Product product = productOpt.get();
        Date now = new Date();
        
        // 创建库存记录
        InventoryRecord record = new InventoryRecord();
        record.setStore(store);
        record.setProduct(product);
        record.setOperator(currentUser);
        record.setRecordTime(now);
        record.setSourceType("manual_adjust");
        record.setSourceId(null); // 手动调整没有关联ID
        record.setBatchNumber(request.getBatchNumber());
        record.setProductionDate(request.getProductionDate());
        record.setExpirationDate(request.getExpirationDate());
        record.setRemark(request.getRemark());
        record.setQuantity(request.getQuantity());
        
        // 查找现有库存
        Optional<StoreInventory> inventoryOpt = storeInventoryRepository.findByStoreAndProduct(store, product);
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
        
        // 根据调整类型更新库存
        if ("increase".equals(request.getAdjustType())) {
            // 增加库存
            record.setRecordType("inbound");
            inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        } else if ("decrease".equals(request.getAdjustType())) {
            // 减少库存
            record.setRecordType("outbound");
            int newQuantity = inventory.getQuantity() - request.getQuantity();
            
            // 检查库存是否足够
            if (newQuantity < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, 400, "库存不足，当前库存: " + inventory.getQuantity()));
            }
            
            inventory.setQuantity(newQuantity);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "无效的调整类型，必须是 'increase' 或 'decrease'"));
        }
        
        // 更新库存最后修改时间
        inventory.setLastUpdateTime(now);
        
        // 保存库存和记录
        storeInventoryRepository.save(inventory);
        InventoryRecord savedRecord = inventoryRecordRepository.save(record);
        
        // 返回响应
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(convertToResponse(savedRecord), 201, "库存调整成功"));
    }
    
    // 入库
    @PostMapping("/inbound")
    @Transactional
    public ResponseEntity<ApiResponse<InventoryRecordResponse>> inboundInventory(
            @RequestBody InventoryAdjustRequest request) {
        
        // 将调整类型设置为增加
        request.setAdjustType("increase");
        
        // 调用调整库存的方法
        return adjustInventory(request);
    }
    
    // 出库
    @PostMapping("/outbound")
    @Transactional
    public ResponseEntity<ApiResponse<InventoryRecordResponse>> outboundInventory(
            @RequestBody InventoryAdjustRequest request) {
        
        // 将调整类型设置为减少
        request.setAdjustType("decrease");
        
        // 调用调整库存的方法
        return adjustInventory(request);
    }
    
    // 获取指定产品在所有门店的库存信息
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getProductInventoryInAllStores(
            @PathVariable Long productId) {
        
        // 检查产品是否存在
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "产品不存在"));
        }
        
        Product product = productOpt.get();
        List<Store> stores = storeRepository.findAll();
        List<InventoryResponse> responseList = new ArrayList<>();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (Store store : stores) {
            // 查询每个门店的该产品库存
            Optional<StoreInventory> inventoryOpt = storeInventoryRepository.findByStoreAndProduct(store, product);
            
            if (inventoryOpt.isPresent()) {
                StoreInventory inventory = inventoryOpt.get();
                
                // 创建响应对象
                InventoryResponse response = new InventoryResponse(
                    inventory.getId(),
                    store.getId(),
                    store.getName(),
                    product.getId(),
                    product.getName(),
                    product.getCode(),
                    product.getCategory() != null ? product.getCategory().toString() : "",
                    product.getSpecification(),
                    product.getManufacturer(),
                    product.getPrice(),
                    product.getDescription(),
                    product.getImage(),
                    inventory.getQuantity(),
                    inventory.getLastUpdateTime() != null ? dateFormat.format(inventory.getLastUpdateTime()) : ""
                );
                
                responseList.add(response);
            } else {
                // 即使没有库存记录，也添加一个库存为0的响应
                InventoryResponse response = new InventoryResponse(
                    null,
                    store.getId(),
                    store.getName(),
                    product.getId(),
                    product.getName(),
                    product.getCode(),
                    product.getCategory() != null ? product.getCategory().toString() : "",
                    product.getSpecification(),
                    product.getManufacturer(),
                    product.getPrice(),
                    product.getDescription(),
                    product.getImage(),
                    0,
                    ""
                );
                
                responseList.add(response);
            }
        }
        
        return ResponseEntity.ok(new ApiResponse<>(responseList, 200, "获取产品在各门店的库存信息成功"));
    }
} 