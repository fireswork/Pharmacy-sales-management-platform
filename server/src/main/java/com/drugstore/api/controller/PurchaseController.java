package com.drugstore.api.controller;

import com.drugstore.api.model.*;
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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseRepository purchaseRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseItemRepository purchaseItemRepository;

    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    // 获取采购列表
    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseResponse>>> getPurchaseList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        
        System.out.println("查询参数: page=" + page + ", size=" + size + ", keyword=" + keyword + ", status=" + status + ", date=" + date);
        
        // 检查页码，确保从0开始
        if (page > 0) {
            System.out.println("注意: 页码从0开始，当前页码为 " + page);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        try {
            // 先简单查询所有记录，确认数据库连接正常
            long totalCount = purchaseRepository.count();
            System.out.println("数据库中总记录数: " + totalCount);
            
            if (totalCount == 0) {
                System.out.println("警告: 数据库中没有采购记录");
                return ResponseEntity.ok(new ApiResponse<>(
                    new PageImpl<>(new ArrayList<>(), pageable, 0),
                    200,
                    "获取采购列表成功 (无数据)"
                ));
            }
            
            // 查询第一页数据，确认能够获取数据
            Page<Purchase> firstPage = purchaseRepository.findAll(PageRequest.of(0, size));
            System.out.println("第一页数据: 总记录数=" + firstPage.getTotalElements() + ", 内容大小=" + firstPage.getContent().size());
            
            // 使用原始查询参数
            Page<Purchase> purchasePage;
            
            // 处理日期范围
            Date startDate = null;
            Date endDate = null;
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                startDate = calendar.getTime();
                
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                endDate = calendar.getTime();
            }
            
            // 根据筛选条件查询
            if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty() && date != null) {
                purchasePage = purchaseRepository.findByKeywordAndStatusAndDate(keyword, status, startDate, endDate, pageable);
            } else if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
                purchasePage = purchaseRepository.findByKeywordAndStatus(keyword, status, pageable);
            } else if (keyword != null && !keyword.isEmpty() && date != null) {
                purchasePage = purchaseRepository.findByKeywordAndDate(keyword, startDate, endDate, pageable);
            } else if (status != null && !status.isEmpty() && date != null) {
                purchasePage = purchaseRepository.findByStatusAndCreateTimeBetween(status, startDate, endDate, pageable);
            } else if (keyword != null && !keyword.isEmpty()) {
                purchasePage = purchaseRepository.findByKeyword(keyword, pageable);
            } else if (status != null && !status.isEmpty()) {
                purchasePage = purchaseRepository.findByStatus(status, pageable);
            } else if (date != null) {
                purchasePage = purchaseRepository.findByCreateTimeBetween(startDate, endDate, pageable);
            } else {
                purchasePage = purchaseRepository.findAll(pageable);
            }
            
            System.out.println("查询结果: 总记录数=" + purchasePage.getTotalElements() + ", 内容大小=" + purchasePage.getContent().size());
            
            if (purchasePage.getContent().isEmpty() && page > 0) {
                System.out.println("警告: 当前页面没有数据，可能是页码超出范围");
                // 如果请求的页码超出范围，返回第一页数据
                page = 0;
                pageable = PageRequest.of(page, size);
                
                // 重新查询第一页
                if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty() && date != null) {
                    purchasePage = purchaseRepository.findByKeywordAndStatusAndDate(keyword, status, startDate, endDate, pageable);
                } else if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
                    purchasePage = purchaseRepository.findByKeywordAndStatus(keyword, status, pageable);
                } else if (keyword != null && !keyword.isEmpty() && date != null) {
                    purchasePage = purchaseRepository.findByKeywordAndDate(keyword, startDate, endDate, pageable);
                } else if (status != null && !status.isEmpty() && date != null) {
                    purchasePage = purchaseRepository.findByStatusAndCreateTimeBetween(status, startDate, endDate, pageable);
                } else if (keyword != null && !keyword.isEmpty()) {
                    purchasePage = purchaseRepository.findByKeyword(keyword, pageable);
                } else if (status != null && !status.isEmpty()) {
                    purchasePage = purchaseRepository.findByStatus(status, pageable);
                } else if (date != null) {
                    purchasePage = purchaseRepository.findByCreateTimeBetween(startDate, endDate, pageable);
                } else {
                    purchasePage = purchaseRepository.findAll(pageable);
                }
                
                System.out.println("重新查询第一页: 总记录数=" + purchasePage.getTotalElements() + ", 内容大小=" + purchasePage.getContent().size());
            }
            
            // 直接查询所有记录（用于调试）
            List<Purchase> allPurchases = purchaseRepository.findAll();
            System.out.println("直接查询所有记录: " + allPurchases.size());
            
            // 使用 JOIN FETCH 查询所有记录（不分页）
            List<Purchase> purchasesWithItems = purchaseRepository.findAllWithItems();
            System.out.println("使用 JOIN FETCH 查询所有记录: " + purchasesWithItems.size());
            
            // 转换为响应对象
            List<PurchaseResponse> responseList = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // 对于分页结果中的每个采购单，单独加载其项目
            for (Purchase purchase : purchasePage.getContent()) {
                // 从带有预加载项目的列表中查找对应的采购单
                Purchase purchaseWithItems = purchasesWithItems.stream()
                    .filter(p -> p.getId().equals(purchase.getId()))
                    .findFirst()
                    .orElse(purchase);
                
                // 添加调试日志
                System.out.println("处理采购单: ID=" + purchaseWithItems.getId() + 
                                  ", 名称=" + purchaseWithItems.getName() + 
                                  ", 项目数量=" + (purchaseWithItems.getItems() != null ? 
                                                purchaseWithItems.getItems().size() : "null"));
                
                List<PurchaseItemResponse> itemResponses = new ArrayList<>();
                
                if (purchaseWithItems.getItems() != null) {
                    for (PurchaseItem item : purchaseWithItems.getItems()) {
                        // 添加调试日志
                        System.out.println("  项目: 产品ID=" + item.getProduct().getId() + 
                                          ", 产品名称=" + item.getProduct().getName() + 
                                          ", 数量=" + item.getQuantity());
                        
                        itemResponses.add(new PurchaseItemResponse(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getRemark() != null ? item.getRemark() : ""
                        ));
                    }
                }
                
                // 添加调试日志
                System.out.println("  转换后的项目数量: " + itemResponses.size());
                
                responseList.add(new PurchaseResponse(
                    purchaseWithItems.getId(),
                    purchaseWithItems.getCode(),
                    purchaseWithItems.getName(),
                    purchaseWithItems.getSupplier() != null ? purchaseWithItems.getSupplier().getId() : null,
                    purchaseWithItems.getApplicant() != null ? purchaseWithItems.getApplicant().getName() : "",
                    purchaseWithItems.getCreateTime() != null ? dateFormat.format(purchaseWithItems.getCreateTime()) : "",
                    purchaseWithItems.getStatus(),
                    purchaseWithItems.getReason() != null ? purchaseWithItems.getReason() : "",
                    purchaseWithItems.getComment() != null ? purchaseWithItems.getComment() : "",
                    itemResponses
                ));
            }
            
            System.out.println("响应列表大小: " + responseList.size());
            
            // 创建分页响应
            Page<PurchaseResponse> responsePage = new PageImpl<>(
                responseList,
                pageable,
                purchasePage.getTotalElements()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(responsePage, 200, "获取采购列表成功"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, 500, "获取采购列表失败: " + e.getMessage()));
        }
    }
    
    // 获取单个采购详情
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getPurchaseById(@PathVariable Long id) {
        Optional<Purchase> purchaseOpt = purchaseRepository.findById(id);
        if (purchaseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "采购申请不存在"));
        }
        
        Purchase purchase = purchaseOpt.get();
        List<PurchaseItemResponse> itemResponses = new ArrayList<>();
        
        if (purchase.getItems() != null) {
            for (PurchaseItem item : purchase.getItems()) {
                itemResponses.add(new PurchaseItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getRemark() != null ? item.getRemark() : ""
                ));
            }
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PurchaseResponse response = new PurchaseResponse(
            purchase.getId(),
            purchase.getCode(),
            purchase.getName(),
            purchase.getSupplier() != null ? purchase.getSupplier().getId() : null,
            purchase.getApplicant() != null ? purchase.getApplicant().getName() : "",
            purchase.getCreateTime() != null ? dateFormat.format(purchase.getCreateTime()) : "",
            purchase.getStatus(),
            purchase.getReason() != null ? purchase.getReason() : "",
            purchase.getComment() != null ? purchase.getComment() : "",
            itemResponses
        );
        
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "获取采购详情成功"));
    }
    
    // 创建采购申请
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponse>> createPurchase(@RequestBody PurchaseRequest request) {
        try {
            // 打印整个请求对象，查看结构
            System.out.println("Request: " + request);
            System.out.println("Products: " + request.getProducts());
            
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(null, 401, "未授权操作"));
            }
            
            // 获取当前用户关联的员工信息
            Employee employee = employeeRepository.findByUser(currentUser);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, 400, "当前用户没有关联的员工信息"));
            }
            
            // 获取供应商
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElse(null);
            if (supplier == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, 400, "供应商不存在"));
            }
            
            // 创建采购单
            Purchase purchase = new Purchase();
            purchase.setCode(generatePurchaseCode());
            purchase.setName(request.getName());
            purchase.setSupplier(supplier);
            purchase.setReason(request.getReason());
            purchase.setStatus("pending");
            purchase.setApplicant(employee);
            purchase.setStore(employee.getStore());
            purchase.setCreateTime(new Date());
            purchase.setUpdateTime(new Date());
            
            // 先保存采购单，获取ID
            Purchase savedPurchase = purchaseRepository.save(purchase);
            
            // 打印保存后的ID，确认不为null
            System.out.println("Saved Purchase ID: " + savedPurchase.getId());
            
            // 处理采购药品
            if (request.getProducts() != null && !request.getProducts().isEmpty()) {
                for (PurchaseItemRequest itemRequest : request.getProducts()) {
                    // 打印整个itemRequest对象
                    System.out.println("Item Request: " + itemRequest);
                    System.out.println("Product ID: " + itemRequest.getProductId());
                    
                    // 如果productId为null，跳过此项
                    if (itemRequest.getProductId() == null) {
                        System.out.println("Skipping item with null productId");
                        continue;
                    }
                    
                    Product product = productRepository.findById(itemRequest.getProductId())
                        .orElse(null);
                    
                    if (product == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiResponse<>(null, 400, "药品不存在，ID: " + itemRequest.getProductId()));
                    }
                    
                    // 打印找到的产品ID，确认不为null
                    System.out.println("Found Product ID: " + product.getId());
                    
                    // 创建并保存每个采购项
                    PurchaseItem item = new PurchaseItem();
                    item.setPurchase(savedPurchase);
                    item.setProduct(product);
                    item.setQuantity(itemRequest.getQuantity());
                    item.setRemark(itemRequest.getRemark());
                    
                    // 单独保存每个采购项
                    purchaseItemRepository.save(item);
                }
                
                // 重新加载采购单以获取关联的采购项
                savedPurchase = purchaseRepository.findById(savedPurchase.getId()).orElse(savedPurchase);
            }
            
            // 构建响应
            List<PurchaseItemResponse> itemResponses = new ArrayList<>();
            for (PurchaseItem item : savedPurchase.getItems()) {
                itemResponses.add(new PurchaseItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getRemark() != null ? item.getRemark() : ""
                ));
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            PurchaseResponse response = new PurchaseResponse(
                savedPurchase.getId(),
                savedPurchase.getCode(),
                savedPurchase.getName(),
                supplier.getId(),
                employee.getName(),
                dateFormat.format(savedPurchase.getCreateTime()),
                savedPurchase.getStatus(),
                savedPurchase.getReason(),
                "",
                itemResponses
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, 201, "采购申请创建成功"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, 500, "创建采购申请失败: " + e.getMessage()));
        }
    }
    
    // 审核采购申请 - 通过
    @Transactional
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseResponse>> approvePurchase(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        
        Optional<Purchase> purchaseOpt = purchaseRepository.findById(id);
        if (purchaseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "采购申请不存在"));
        }
        
        Purchase purchase = purchaseOpt.get();
        if (!"pending".equals(purchase.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "只能审核待审核状态的采购申请"));
        }
        
        purchase.setStatus("approved");
        purchase.setComment(comment);
        purchase.setUpdateTime(new Date());
        
        Purchase savedPurchase = purchaseRepository.save(purchase);
        
        // 更新门店库存
        updateStoreInventory(savedPurchase);
        
        // 构建响应
        List<PurchaseItemResponse> itemResponses = new ArrayList<>();
        for (PurchaseItem item : savedPurchase.getItems()) {
            itemResponses.add(new PurchaseItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getRemark() != null ? item.getRemark() : ""
            ));
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PurchaseResponse response = new PurchaseResponse(
            savedPurchase.getId(),
            savedPurchase.getCode(),
            savedPurchase.getName(),
            savedPurchase.getSupplier() != null ? savedPurchase.getSupplier().getId() : null,
            savedPurchase.getApplicant() != null ? savedPurchase.getApplicant().getName() : "",
            savedPurchase.getCreateTime() != null ? dateFormat.format(savedPurchase.getCreateTime()) : "",
            savedPurchase.getStatus(),
            savedPurchase.getReason() != null ? savedPurchase.getReason() : "",
            savedPurchase.getComment() != null ? savedPurchase.getComment() : "",
            itemResponses
        );
        
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "采购申请已通过"));
    }
    
    // 审核采购申请 - 拒绝
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PurchaseResponse>> rejectPurchase(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        
        Optional<Purchase> purchaseOpt = purchaseRepository.findById(id);
        if (purchaseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "采购申请不存在"));
        }
        
        Purchase purchase = purchaseOpt.get();
        if (!"pending".equals(purchase.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(null, 400, "只能审核待审核状态的采购申请"));
        }
        
        purchase.setStatus("rejected");
        purchase.setComment(comment);
        purchase.setUpdateTime(new Date());
        
        Purchase savedPurchase = purchaseRepository.save(purchase);
        
        // 构建响应
        List<PurchaseItemResponse> itemResponses = new ArrayList<>();
        for (PurchaseItem item : savedPurchase.getItems()) {
            itemResponses.add(new PurchaseItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getRemark() != null ? item.getRemark() : ""
            ));
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PurchaseResponse response = new PurchaseResponse(
            savedPurchase.getId(),
            savedPurchase.getCode(),
            savedPurchase.getName(),
            savedPurchase.getSupplier() != null ? savedPurchase.getSupplier().getId() : null,
            savedPurchase.getApplicant() != null ? savedPurchase.getApplicant().getName() : "",
            savedPurchase.getCreateTime() != null ? dateFormat.format(savedPurchase.getCreateTime()) : "",
            savedPurchase.getStatus(),
            savedPurchase.getReason() != null ? savedPurchase.getReason() : "",
            savedPurchase.getComment() != null ? savedPurchase.getComment() : "",
            itemResponses
        );
        
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "采购申请已拒绝"));
    }
    
    // 生成采购编号
    private String generatePurchaseCode() {
        long count = purchaseRepository.count();
        return "PUR" + String.format("%04d", count + 1);
    }

    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updatePurchase(
            @PathVariable Long id, 
            @RequestBody StatusRequest request
    ) {
        try {
            Optional<Purchase> existingPurchase = purchaseRepository.findById(id);
            
            if (existingPurchase.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(null, 404, "采购申请不存在"));
            }
            
            Purchase purchase = existingPurchase.get();
            String oldStatus = purchase.getStatus();
            String newStatus = request.getStatus();
            
            // 更新状态和评论
            purchase.setStatus(newStatus);
            purchase.setComment(request.getComment());
            purchase.setUpdateTime(new Date());
            
            Purchase updatedPurchase = purchaseRepository.save(purchase);
            
            // 如果状态从 pending 变为 approved，则更新库存
            if ("pending".equals(oldStatus) && "approved".equals(newStatus)) {
                updateStoreInventory(updatedPurchase);
            }
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            PurchaseResponse response = new PurchaseResponse(
                updatedPurchase.getId(),
                updatedPurchase.getCode(),
                updatedPurchase.getName(),
                updatedPurchase.getSupplier() != null ? updatedPurchase.getSupplier().getId() : null,
                updatedPurchase.getApplicant() != null ? updatedPurchase.getApplicant().getName() : "",
                updatedPurchase.getCreateTime() != null ? dateFormat.format(updatedPurchase.getCreateTime()) : "",
                updatedPurchase.getStatus(),
                updatedPurchase.getReason() != null ? updatedPurchase.getReason() : "",
                updatedPurchase.getComment() != null ? updatedPurchase.getComment() : "",
                updatedPurchase.getItems().stream()
                    .map(item -> new PurchaseItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getRemark() != null ? item.getRemark() : ""
                    )).collect(Collectors.toList())
            );
            
            return ResponseEntity.ok(new ApiResponse<>(response, 200, "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, 500, "更新失败：" + e.getMessage()));
        }
    }

    // 更新门店库存的方法
    private void updateStoreInventory(Purchase purchase) {
        if (purchase.getStore() == null) {
            System.out.println("警告: 采购单没有关联门店，无法更新库存");
            return;
        }
        
        Store store = purchase.getStore();
        Date now = new Date();
        
        for (PurchaseItem item : purchase.getItems()) {
            Product product = item.getProduct();
            Integer quantity = item.getQuantity();
            
            // 查找是否已有该药品的库存记录
            Optional<StoreInventory> inventoryOpt = storeInventoryRepository.findByStoreAndProduct(store, product);
            
            if (inventoryOpt.isPresent()) {
                // 更新现有库存
                StoreInventory inventory = inventoryOpt.get();
                inventory.setQuantity(inventory.getQuantity() + quantity);
                inventory.setLastUpdateTime(now);
                storeInventoryRepository.save(inventory);
                System.out.println("更新门店 " + store.getName() + " 的药品 " + product.getName() + " 库存，新数量: " + inventory.getQuantity());
            } else {
                // 创建新的库存记录
                StoreInventory newInventory = new StoreInventory();
                newInventory.setStore(store);
                newInventory.setProduct(product);
                newInventory.setQuantity(quantity);
                newInventory.setLastUpdateTime(now);
                storeInventoryRepository.save(newInventory);
                System.out.println("为门店 " + store.getName() + " 添加药品 " + product.getName() + " 库存，数量: " + quantity);
            }
        }
    }
} 