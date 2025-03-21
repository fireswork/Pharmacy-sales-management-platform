package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepository;

    /**
     * 获取供应商列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Supplier>>> getAllSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        try {
            Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sorting = Sort.by(direction, sort);
            Pageable pageable = PageRequest.of(page, size, sorting);

            Specification<Supplier> specification = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (keyword != null && !keyword.isEmpty()) {
                    Predicate namePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            "%" + keyword.toLowerCase() + "%");
                    Predicate codePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("code")),
                            "%" + keyword.toLowerCase() + "%");
                    predicates.add(criteriaBuilder.or(namePredicate, codePredicate));
                }

                if (status != null && !status.isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            Page<Supplier> suppliers = supplierRepository.findAll(specification, pageable);
            PageResponse<Supplier> pageResponse = new PageResponse<>(
                suppliers.getContent(),
                suppliers.getTotalElements(),
                suppliers.getTotalPages(),
                suppliers.getNumber(),
                suppliers.getSize()
            );

            return ResponseEntity.ok(new ApiResponse<>(pageResponse, 200, "获取供应商列表成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "获取供应商列表失败: " + e.getMessage()));
        }
    }

    /**
     * 添加供应商
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        try {
            // 生成供应商编号
            String code = generateSupplierCode();
            supplier.setCode(code);
            
            Supplier savedSupplier = supplierRepository.save(supplier);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(savedSupplier, 201, "添加供应商成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "添加供应商失败: " + e.getMessage()));
        }
    }

    /**
     * 更新供应商信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(
            @PathVariable Long id,
            @RequestBody Supplier supplierRequest
    ) {
        try {
            Optional<Supplier> existingSupplier = supplierRepository.findById(id);
            
            if (existingSupplier.isPresent()) {
                Supplier supplier = existingSupplier.get();
                supplier.setName(supplierRequest.getName());
                supplier.setContactPerson(supplierRequest.getContactPerson());
                supplier.setPhone(supplierRequest.getPhone());
                supplier.setEmail(supplierRequest.getEmail());
                supplier.setAddress(supplierRequest.getAddress());
                supplier.setBusinessScope(supplierRequest.getBusinessScope());
                supplier.setRemark(supplierRequest.getRemark());
                
                Supplier updatedSupplier = supplierRepository.save(supplier);
                return ResponseEntity.ok(new ApiResponse<>(updatedSupplier, 200, "更新供应商成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "供应商不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "更新供应商失败: " + e.getMessage()));
        }
    }

    /**
     * 更新供应商状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplierStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest statusRequest
    ) {
        try {
            Optional<Supplier> existingSupplier = supplierRepository.findById(id);
            
            if (existingSupplier.isPresent()) {
                supplierRepository.updateStatus(id, statusRequest.getStatus());
                
                Supplier updatedSupplier = supplierRepository.findById(id).get();
                return ResponseEntity.ok(new ApiResponse<>(updatedSupplier, 200, "更新供应商状态成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(null, 404, "供应商不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null, 500, "更新供应商状态失败: " + e.getMessage()));
        }
    }

    /**
     * 生成供应商编号
     */
    private String generateSupplierCode() {
        // 获取当前供应商数量
        long count = supplierRepository.count();
        // 生成编号：SP + 4位数字（从0001开始）
        return "SP" + String.format("%04d", count + 1);
    }
} 