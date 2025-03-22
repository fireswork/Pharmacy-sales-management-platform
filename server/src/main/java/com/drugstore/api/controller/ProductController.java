package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    @Autowired
    private InventoryRecordRepository inventoryRecordRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取药品列表（支持分页、排序和筛选）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Product>>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        try {
            // 创建排序对象
            Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sorting = Sort.by(direction, sort);
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page, size, sorting);
            
            // 创建动态查询条件
            Specification<Product> specification = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // 关键字搜索（药品名称或编号）
                if (keyword != null && !keyword.isEmpty()) {
                    Predicate namePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")), 
                            "%" + keyword.toLowerCase() + "%");
                    Predicate codePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("code")), 
                            "%" + keyword.toLowerCase() + "%");
                    predicates.add(criteriaBuilder.or(namePredicate, codePredicate));
                }
                
                // 分类筛选
                if (category != null && !category.isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("category"), category));
                }
                
                // 状态筛选
                if (status != null && !status.isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }
                
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
            
            // 执行查询
            Page<Product> products = productRepository.findAll(specification, pageable);
            
            // 构建分页响应
            PageResponse<Product> pageResponse = new PageResponse<>(
                products.getContent(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.getNumber(),
                products.getSize()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(pageResponse, 200, "获取药品列表成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "获取药品列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取单个药品详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        try {
            Optional<Product> product = productRepository.findById(id);
            
            if (product.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(product.get(), 200, "获取药品详情成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "药品不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "获取药品详情失败: " + e.getMessage()));
        }
    }

    /**
     * 添加新药品
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody ProductRequest productRequest) {
        try {
            // 创建新药品对象
            Product product = new Product();
            product.setName(productRequest.getName());
            
            // 生成药品编号
            String code = generateProductCode();
            product.setCode(code);
            
            product.setCategory(productRequest.getCategory());
            product.setSpecification(productRequest.getSpecification());
            product.setManufacturer(productRequest.getManufacturer());
            product.setSupplier(productRequest.getSupplier());
            product.setApprovalNumber(productRequest.getApprovalNumber());
            product.setRetailPrice(productRequest.getRetailPrice());
            product.setCostPrice(productRequest.getCostPrice());
            
            // 设置 price 字段，使用 retailPrice 的值
            if (productRequest.getRetailPrice() != null) {
                product.setPrice(productRequest.getRetailPrice());
            } else {
                product.setPrice(BigDecimal.ZERO);
            }
            
            product.setImage(productRequest.getImage());
            product.setDescription(productRequest.getDescription());
            product.setUsage(productRequest.getUsage());
            product.setStatus("active");
            
            // 保存药品
            Product savedProduct = productRepository.save(product);
            
            // 创建门店库存记录
            Long storeId = productRequest.getStoreId();
            if (storeId == null) {
                Store userStore = getCurrentUserStore();
                if (userStore != null) {
                    storeId = userStore.getId();
                }
            }
            
            if (storeId != null) {
                createStoreInventory(savedProduct, storeId, productRequest.getStock());
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(savedProduct, 201, "添加药品成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "添加药品失败: " + e.getMessage()));
        }
    }

    /**
     * 生成唯一的药品编号
     */
    private String generateProductCode() {
        // 获取当前药品数量
        long count = productRepository.count();
        // 生成编号：P + 6位数字（从000001开始）
        return "P" + String.format("%06d", count + 1);
    }

    /**
     * 创建门店库存记录
     */
    private void createStoreInventory(Product product, Long storeId, Integer quantity) {
        try {
            // 查找是否有此门店
            Optional<Store> storeOpt = storeRepository.findById(storeId);
            if (storeOpt.isEmpty()) {
                throw new RuntimeException("门店不存在，ID: " + storeId);
            }
            
            Store store = storeOpt.get();
            Date now = new Date();
            
            // 创建新的库存记录
            StoreInventory newInventory = new StoreInventory();
            newInventory.setStore(store);
            newInventory.setProduct(product);
            newInventory.setQuantity(quantity);
            newInventory.setLastUpdateTime(now);
            StoreInventory savedInventory = storeInventoryRepository.save(newInventory);
            
            if (savedInventory == null) {
                throw new RuntimeException("保存库存记录失败");
            }
            
            // 增加创建库存记录
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                InventoryRecord record = new InventoryRecord();
                record.setStore(store);
                record.setProduct(product);
                record.setRecordType("inbound");
                record.setQuantity(quantity);
                record.setRecordTime(now);
                record.setSourceType("new_product");
                record.setRemark("新增药品入库");
                record.setOperator(currentUser);
                
                // 保存记录
                inventoryRecordRepository.save(record);
            }
        } catch (Exception e) {
            throw new RuntimeException("创建库存记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户关联的门店
     */
    private Store getCurrentUserStore() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return null;
            
            Employee employee = employeeRepository.findByUser(currentUser);
            if (employee == null) return null;
            
            return employee.getStore();
        } catch (Exception e) {
            System.out.println("获取当前用户门店失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取当前用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            System.out.println("获取当前用户失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 更新药品信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id, 
            @RequestBody ProductRequest productRequest
    ) {
        try {
            Optional<Product> existingProduct = productRepository.findById(id);
            
            if (existingProduct.isPresent()) {
                Product product = existingProduct.get();
                
                // 不更新药品编号，其他信息都更新
                product.setName(productRequest.getName());
                product.setCategory(productRequest.getCategory());
                product.setSpecification(productRequest.getSpecification());
                product.setManufacturer(productRequest.getManufacturer());
                product.setSupplier(productRequest.getSupplier());
                product.setApprovalNumber(productRequest.getApprovalNumber());
                product.setRetailPrice(productRequest.getRetailPrice());
                product.setCostPrice(productRequest.getCostPrice());
                
                // 设置 price 字段，使用 retailPrice 的值
                if (productRequest.getRetailPrice() != null) {
                    product.setPrice(productRequest.getRetailPrice());
                }
                
                product.setImage(productRequest.getImage());
                product.setDescription(productRequest.getDescription());
                
                // 设置库存，如果为null则保持原值
                if (productRequest.getStock() != null) {
                    product.setStock(productRequest.getStock());
                }
                
                // 使用 setter 方法设置 usage 字段
                if (productRequest.getUsage() != null) {
                    product.setUsage(productRequest.getUsage());
                }
                
                product.setStatus(productRequest.getStatus());
                
                // 保存更新后的药品
                Product updatedProduct = productRepository.save(product);
                
                return ResponseEntity.ok(new ApiResponse<>(updatedProduct, 200, "更新药品成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "药品不存在"));
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细错误信息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "更新药品失败: " + e.getMessage()));
        }
    }

    /**
     * 更新药品状态（上架/下架）
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Product>> updateProductStatus(
            @PathVariable Long id, 
            @RequestBody StatusRequest statusRequest
    ) {
        try {
            Optional<Product> existingProduct = productRepository.findById(id);
            
            if (existingProduct.isPresent()) {
                // 使用原生查询更新状态
                productRepository.updateStatus(id, statusRequest.getStatus());
                
                // 重新获取更新后的产品
                Product updatedProduct = productRepository.findById(id).get();
                
                return ResponseEntity.ok(new ApiResponse<>(updatedProduct, 200, "更新药品状态成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "药品不存在"));
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细错误信息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "更新药品状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除药品
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            Optional<Product> existingProduct = productRepository.findById(id);
            
            if (existingProduct.isPresent()) {
                // 删除药品
                productRepository.deleteById(id);
                
                return ResponseEntity.ok(new ApiResponse<>(null, 200, "删除药品成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "药品不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "删除药品失败: " + e.getMessage()));
        }
    }
} 