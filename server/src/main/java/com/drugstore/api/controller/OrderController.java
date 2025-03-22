package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import com.drugstore.api.model.request.OrderRequest;
import com.drugstore.api.model.response.OrderResponse;
import com.drugstore.api.model.response.OrderItemResponse;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    // 创建订单
    @PostMapping
    @Transactional  // 添加事务注解，确保库存和订单操作的原子性
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody OrderRequest request) {
        User user = getCurrentUser();
        
        // 1. 验证购物车商品
        List<CartItem> cartItems = cartItemRepository.findByUserAndSelected(user, true);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("请选择要购买的商品");
        }
        
        // 2. 验证门店库存
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            Store store = item.getStore();
            
            // 从门店库存表中查询库存
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(
                store.getId(), product.getId())
                .orElseThrow(() -> new RuntimeException("商品 " + product.getName() + " 在门店 " + store.getName() + " 没有库存记录"));
            
            System.out.println("检查商品库存: " + product.getName() + 
                             ", 门店: " + store.getName() +
                             ", 门店库存: " + inventory.getQuantity() + 
                             ", 购买数量: " + item.getQuantity());
            
            if (inventory.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("商品 " + product.getName() + 
                                         " 在门店 " + store.getName() +
                                         " 库存不足 (当前库存: " + inventory.getQuantity() + 
                                         ", 需要数量: " + item.getQuantity() + ")");
            }
        }
        
        // 3. 获取地址信息
        Address address = null;
        if (request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("收货地址不存在"));
        } else if (request.getReceiverName() != null && request.getReceiverPhone() != null && request.getDeliveryAddress() != null) {
            // 如果前端直接传了地址信息，则使用传递的信息
        } else {
            throw new RuntimeException("请提供有效的收货地址");
        }
        
        // 4. 按商店分组创建多个订单
        Map<Store, List<CartItem>> cartItemsByStore = cartItems.stream()
            .collect(Collectors.groupingBy(CartItem::getStore));
        
        List<Order> createdOrders = new ArrayList<>();
        
        for (Map.Entry<Store, List<CartItem>> entry : cartItemsByStore.entrySet()) {
            Store store = entry.getKey();
            List<CartItem> storeItems = entry.getValue();
            
            // 创建订单
            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setUser(user);
            order.setStore(store);
            order.setTotalAmount(calculateTotal(storeItems));
            order.setDeliveryMethod(request.getDeliveryMethod());
            order.setPaymentMethod(request.getPaymentMethod());
            
            // 设置收货信息
            if (address != null) {
                order.setReceiverName(address.getReceiver());
                order.setReceiverPhone(address.getPhoneNumber());
                order.setDeliveryAddress(address.getAddress());
            } else {
                order.setReceiverName(request.getReceiverName());
                order.setReceiverPhone(request.getReceiverPhone());
                order.setDeliveryAddress(request.getDeliveryAddress());
            }
            
            // 直接设置为已付款状态
            order.setStatus("PAID");
            order.setCreateTime(new Date());
            order.setPaymentTime(new Date());
            
            // 创建订单项并减少库存
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : storeItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getProduct().getPrice());
                orderItems.add(orderItem);
                
                // 减少门店商品库存
                Product product = cartItem.getProduct();
                Store itemStore = cartItem.getStore();
                
                // 获取门店库存并减少
                StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(
                    itemStore.getId(), product.getId())
                    .orElseThrow(() -> new RuntimeException("商品库存记录不存在"));
                
                inventory.setQuantity(inventory.getQuantity() - cartItem.getQuantity());
                inventory.setLastUpdateTime(new Date()); // 更新库存修改时间
                storeInventoryRepository.save(inventory);
            }
            
            order.setItems(orderItems);
            
            // 保存订单
            orderRepository.save(order);
            createdOrders.add(order);
        }
        
        // 清除已购买的购物车项
        cartItemRepository.deleteAll(cartItems);
        
        // 返回第一个订单的信息(如果需要可以修改为返回所有订单)
        return ResponseEntity.ok(new ApiResponse<>(convertToResponse(createdOrders.get(0)), 200, "订单创建成功，已自动完成支付"));
    }

    // 获取订单列表
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        User user = getCurrentUser();
        List<Order> orders = orderRepository.findByUserOrderByCreateTimeDesc(user);
        List<OrderResponse> response = orders.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(response, 200, "获取成功"));
    }

    // 获取订单详情
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        return ResponseEntity.ok(new ApiResponse<>(convertToResponse(order), 200, "获取成功"));
    }

    // 更新订单状态
    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 如果订单被取消，且之前的状态是已付款，则需要恢复库存
        if (status.equals("CANCELLED") && order.getStatus().equals("PAID")) {
            restoreProductStock(order);
        }

        order.setStatus(status);
        switch (status) {
            case "PAID":
                order.setPaymentTime(new Date());
                break;
            case "DELIVERING":
                order.setDeliveryTime(new Date());
                break;
            case "COMPLETED":
                order.setCompleteTime(new Date());
                break;
        }

        orderRepository.save(order);
        return ResponseEntity.ok(new ApiResponse<>(null, 200, "更新成功"));
    }

    // 恢复商品库存（订单取消时调用）
    private void restoreProductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Store store = order.getStore();
            
            // 获取门店库存并恢复
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(
                store.getId(), product.getId())
                .orElseThrow(() -> new RuntimeException("商品库存记录不存在"));
            
            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            inventory.setLastUpdateTime(new Date()); // 更新库存修改时间
            storeInventoryRepository.save(inventory);
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username);
    }

    private String generateOrderNumber() {
        return "ORDER" + System.currentTimeMillis();
    }

    private BigDecimal calculateTotal(List<CartItem> cartItems) {
        return cartItems.stream()
            .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse convertToResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            order.getDeliveryMethod(),
            order.getPaymentMethod(),
            order.getStatus(),
            order.getCreateTime(),
            order.getItems().stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList())
        );
    }

    private OrderItemResponse convertToItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getImage(),
            item.getPrice(),
            item.getQuantity()
        );
    }
} 