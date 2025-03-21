package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreInventoryRepository inventoryRepository;

    // 获取收藏列表
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getUserFavorites(
            @RequestParam Long storeId) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        List<Favorite> favorites = favoriteRepository.findByUserIdAndStoreId(user.getId(), storeId);
        
        List<FavoriteResponse> responseList = favorites.stream()
            .map(favorite -> {
                Product product = favorite.getProduct();
                Store store = favorite.getStore();
                
                // 检查商品是否可用
                boolean isAvailable = "active".equals(product.getStatus());
                
                // 检查当前门店是否有该商品库存
                Optional<StoreInventory> inventory = 
                    inventoryRepository.findByStoreIdAndProductId(storeId, product.getId());
                
                return new FavoriteResponse(
                    favorite.getId(),
                    product.getId(),
                    product.getName(),
                    product.getCode(),
                    product.getSpecification(),
                    product.getManufacturer(),
                    product.getPrice(),
                    product.getImage(),
                    isAvailable,
                    inventory.isPresent(),
                    inventory.map(StoreInventory::getQuantity).orElse(0),
                    store.getId().toString(),
                    store.getName()
                );
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(responseList, 200, "获取收藏列表成功"));
    }

    // 添加收藏
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long productId,
            @RequestParam Long storeId) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        if (favoriteRepository.existsByUserIdAndStoreIdAndProductId(user.getId(), storeId, productId)) {
            return ResponseEntity.ok(new ApiResponse<>(null, 400, "该商品已在收藏列表中"));
        }

        Optional<Product> productOpt = productRepository.findById(productId);
        Optional<Store> storeOpt = storeRepository.findById(storeId);
        
        if (productOpt.isEmpty() || storeOpt.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(null, 404, "商品或门店不存在"));
        }

        Product product = productOpt.get();
        if (!"active".equals(product.getStatus())) {
            return ResponseEntity.ok(new ApiResponse<>(null, 400, "该商品已下架"));
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setStore(storeOpt.get());
        favorite.setProduct(product);
        favorite.setCreateTime(new Date());
        
        favoriteRepository.save(favorite);
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "添加收藏成功"));
    }

    // 取消收藏
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long productId,
            @RequestParam Long storeId) {
        // 获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        
        Optional<Favorite> favoriteOpt = 
            favoriteRepository.findByUserIdAndStoreIdAndProductId(user.getId(), storeId, productId);
            
        if (favoriteOpt.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(null, 404, "收藏记录不存在"));
        }

        favoriteRepository.delete(favoriteOpt.get());
        
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "取消收藏成功"));
    }
} 