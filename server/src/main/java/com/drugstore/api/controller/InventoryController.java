package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.StoreInventoryRepository;
import com.drugstore.api.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private StoreInventoryRepository inventoryRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    // 获取门店库存列表
    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getStoreInventory(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        
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
        
        // 如果有分类ID，过滤结果
        if (categoryId != null) {
            inventoryList = inventoryList.stream()
                .filter(inv -> {
                    Product product = inv.getProduct();
                    return product.getCategory() != null && 
                           categoryId.toString().equals(product.getCategory());
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
} 