package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.model.request.CartRequest;
import com.drugstore.api.model.response.CartResponse;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreInventoryRepository inventoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 获取购物车列表
    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<List<CartResponse>>> getCart(
            @PathVariable Long storeId
    ) {
        User user = getCurrentUser();
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("门店不存在"));

        List<CartItem> cartItems = cartItemRepository.findByUserAndStoreOrderByCreateTimeDesc(user, store);
        List<CartResponse> response = cartItems.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(response, 200, "获取成功"));
    }

    // 添加到购物车
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addToCart(@RequestBody CartRequest request) {
        User user = getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("商品不存在"));
        Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new RuntimeException("门店不存在"));

        CartItem cartItem = cartItemRepository
            .findByUserAndStoreAndProduct(user, store, product)
            .orElse(new CartItem());

        cartItem.setUser(user);
        cartItem.setStore(store);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setSelected(true);

        cartItemRepository.save(cartItem);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "添加成功"));
    }

    // 更新购物车数量
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCart(
            @PathVariable Long id,
            @RequestBody CartRequest request) {
        CartItem cartItem = cartItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("购物车项不存在"));

        // 检查库存
        Optional<StoreInventory> inventory = inventoryRepository.findByStoreIdAndProductId(
            cartItem.getStore().getId(), 
            cartItem.getProduct().getId()
        );

        if (inventory.isEmpty()) {
            throw new RuntimeException("商品库存不存在");
        }

        if (request.getQuantity() > inventory.get().getQuantity()) {
            throw new RuntimeException("商品库存不足");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "更新成功"));
    }

    // 删除购物车项
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@PathVariable Long id) {
        cartItemRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "删除成功"));
    }

    // 选择/取消选择购物车项
    @PutMapping("/{id}/select")
    public ResponseEntity<ApiResponse<Void>> selectCartItem(
            @PathVariable Long id,
            @RequestParam Boolean selected) {
        CartItem cartItem = cartItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("购物车项不存在"));

        cartItem.setSelected(selected);
        cartItemRepository.save(cartItem);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "更新成功"));
    }

    // 清空购物车
    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> clearCart(@RequestParam Long storeId) {
        User user = getCurrentUser();
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("门店不存在"));

        cartItemRepository.deleteByUserAndStore(user, store);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "购物车已清空"));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    private CartResponse convertToResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        Optional<StoreInventory> inventory = inventoryRepository.findByStoreIdAndProductId(
            cartItem.getStore().getId(), 
            product.getId()
        );
        
        int stockQuantity = inventory.map(StoreInventory::getQuantity).orElse(0);
        boolean inStock = stockQuantity > 0;
        
        // 检查用户是否为会员并获取会员折扣价格
        BigDecimal price = product.getPrice();
        BigDecimal memberPrice = null;
        boolean isMember = false;
        
        // 获取用户的会员信息
        User user = cartItem.getUser();
        if (user != null) {
            Member member = memberRepository.findByUser(user);
            if (member != null && member.getIsRegistered() != null && member.getIsRegistered()) {
                isMember = true;
                // 会员价格打9折
                memberPrice = price.multiply(new BigDecimal("0.9")).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }

        return new CartResponse(
            cartItem.getId(),
            product.getId(),
            product.getName(),
            product.getCode(),
            product.getSpecification(),
            product.getManufacturer(),
            price,
            product.getImage(),
            cartItem.getQuantity(),
            cartItem.getSelected(),
            "active".equals(product.getStatus()),
            inStock,
            product.getPrescription(),
            stockQuantity,
            isMember,
            memberPrice
        );
    }
} 