package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.drugstore.api.model.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private StoreRepository storeRepository;

    // 获取所有门店（带分页、搜索和状态筛选）
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoreInfoResponse>>> getAllStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Store> storePage;
        
        // 根据搜索条件和状态筛选查询
        if (keyword != null && !keyword.isEmpty() && status != null && !status.isEmpty()) {
            storePage = storeRepository.findByStatusAndNameContainingOrCodeContaining(status, keyword, keyword, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            storePage = storeRepository.findByNameContainingOrCodeContaining(keyword, keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            storePage = storeRepository.findByStatus(status, pageable);
        } else {
            storePage = storeRepository.findAll(pageable);
        }
        
        // 转换为响应对象
        List<StoreInfoResponse> storeResponses = storePage.getContent().stream()
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
        
        // 创建分页响应
        Page<StoreInfoResponse> storeResponsePage = new PageImpl<>(
            storeResponses, pageable, storePage.getTotalElements());
        
        return ResponseEntity.ok(new ApiResponse<>(storeResponsePage, 200, "获取门店列表成功"));
    }

    // 获取单个门店
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<StoreInfoResponse>> getStoreByCode(@PathVariable String code) {
        Store store = storeRepository.findByCode(code);
        
        if (store == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        StoreInfoResponse storeResponse = new StoreInfoResponse(
            store.getId(),
            store.getCode(),
            store.getName(),
            store.getAddress() != null ? store.getAddress() : "",
            store.getPhoneNumber() != null ? store.getPhoneNumber() : "",
            store.getOpenTime() != null ? store.getOpenTime() : "",
            store.getCloseTime() != null ? store.getCloseTime() : "",
            store.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(storeResponse, 200, "获取门店信息成功"));
    }

    // 添加门店
    @PostMapping
    public ResponseEntity<ApiResponse<StoreInfoResponse>> addStore(@RequestBody StoreRequest storeRequest) {
        // 生成门店编号
        String code = generateStoreCode();
        
        // 创建新门店
        Store newStore = new Store();
        newStore.setCode(code);
        newStore.setName(storeRequest.getName());
        newStore.setPhoneNumber(storeRequest.getPhoneNumber());
        newStore.setOpenTime(storeRequest.getOpenTime());
        newStore.setCloseTime(storeRequest.getCloseTime());
        newStore.setAddress(storeRequest.getAddress());
        newStore.setStatus(storeRequest.getStatus() != null ? storeRequest.getStatus() : "active");
        
        // 保存门店
        Store savedStore = storeRepository.save(newStore);
        
        // 构建响应
        StoreInfoResponse storeResponse = new StoreInfoResponse(
            savedStore.getId(),
            savedStore.getCode(),
            savedStore.getName(),
            savedStore.getAddress() != null ? savedStore.getAddress() : "",
            savedStore.getPhoneNumber() != null ? savedStore.getPhoneNumber() : "",
            savedStore.getOpenTime() != null ? savedStore.getOpenTime() : "",
            savedStore.getCloseTime() != null ? savedStore.getCloseTime() : "",
            savedStore.getStatus()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(storeResponse, 201, "门店添加成功"));
    }

    // 生成门店编号
    private String generateStoreCode() {
        // 获取当前门店数量
        long storeCount = storeRepository.count();
        
        // 生成新的门店编号，格式为 S001, S002, ...
        return "S" + String.format("%03d", storeCount + 1);
    }

    // 更新门店（通过ID）
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreInfoResponse>> updateStoreById(
            @PathVariable Long id,
            @RequestBody StoreRequest storeRequest) {
        
        // 查找门店
        Store store = storeRepository.findById(id).orElse(null);
        
        if (store == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        // 更新门店信息
        if (storeRequest.getName() != null) {
            store.setName(storeRequest.getName());
        }
        if (storeRequest.getPhoneNumber() != null) {
            store.setPhoneNumber(storeRequest.getPhoneNumber());
        }
        if (storeRequest.getOpenTime() != null) {
            store.setOpenTime(storeRequest.getOpenTime());
        }
        if (storeRequest.getCloseTime() != null) {
            store.setCloseTime(storeRequest.getCloseTime());
        }
        if (storeRequest.getAddress() != null) {
            store.setAddress(storeRequest.getAddress());
        }
        if (storeRequest.getStatus() != null) {
            store.setStatus(storeRequest.getStatus());
        }
        
        // 保存门店
        Store savedStore = storeRepository.save(store);
        
        // 构建响应
        StoreInfoResponse storeResponse = new StoreInfoResponse(
            savedStore.getId(),
            savedStore.getCode(),
            savedStore.getName(),
            savedStore.getAddress() != null ? savedStore.getAddress() : "",
            savedStore.getPhoneNumber() != null ? savedStore.getPhoneNumber() : "",
            savedStore.getOpenTime() != null ? savedStore.getOpenTime() : "",
            savedStore.getCloseTime() != null ? savedStore.getCloseTime() : "",
            savedStore.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(storeResponse, 200, "门店信息更新成功"));
    }

    // 更新门店状态（通过ID）
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<StoreInfoResponse>> updateStoreStatusById(
            @PathVariable Long id,
            @RequestParam String status) {
        
        // 查找门店
        Store store = storeRepository.findById(id).orElse(null);
        
        if (store == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(null, 404, "门店不存在"));
        }
        
        // 更新状态
        store.setStatus(status);
        Store savedStore = storeRepository.save(store);
        
        // 构建响应
        StoreInfoResponse storeResponse = new StoreInfoResponse(
            savedStore.getId(),
            savedStore.getCode(),
            savedStore.getName(),
            savedStore.getAddress() != null ? savedStore.getAddress() : "",
            savedStore.getPhoneNumber() != null ? savedStore.getPhoneNumber() : "",
            savedStore.getOpenTime() != null ? savedStore.getOpenTime() : "",
            savedStore.getCloseTime() != null ? savedStore.getCloseTime() : "",
            savedStore.getStatus()
        );
        
        return ResponseEntity.ok(new ApiResponse<>(storeResponse, 200, "门店状态更新成功"));
    }

    // 获取所有门店（不分页，用于下拉选择）
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<StoreInfoResponse>>> getAllStoresForSelect() {
        List<Store> stores = storeRepository.findByStatus("active");
        
        List<StoreInfoResponse> storeResponses = stores.stream()
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
        
        return ResponseEntity.ok(new ApiResponse<>(storeResponses, 200, "获取所有门店成功"));
    }
} 