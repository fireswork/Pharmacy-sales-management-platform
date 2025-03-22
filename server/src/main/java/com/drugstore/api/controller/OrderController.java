package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;

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

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InventoryRecordRepository inventoryRecordRepository;

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
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().toUpperCase().equals("ADMIN");
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        // 使用动态查询条件
        Specification<Order> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 如果不是管理员，只能查看自己的订单
            if (!isAdmin) {
                predicates.add(criteriaBuilder.equal(root.get("user"), currentUser));
            }
            
            // 按关键字搜索（订单号或会员名）
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(root.get("orderNumber"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("receiverName"), "%" + keyword + "%")
                ));
            }
            
            // 按状态过滤
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            // 按店铺过滤
            if (storeId != null) {
                Join<Order, Store> storeJoin = root.join("store", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(storeJoin.get("id"), storeId));
            }
            
            // 按日期范围过滤
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date start = dateFormat.parse(startDate);
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createTime"), start));
                } catch (ParseException e) {
                    System.err.println("Invalid start date format: " + startDate);
                }
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date end = dateFormat.parse(endDate);
                    // 将结束日期设置为当天的23:59:59
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(end);
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createTime"), calendar.getTime()));
                } catch (ParseException e) {
                    System.err.println("Invalid end date format: " + endDate);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 查询数据
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        
        // 转换为响应对象
        List<OrderResponse> responseList = orderPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        // 构建包含分页信息的响应
        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(responseList, 200, "获取成功");
        response.setTotal(orderPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        
        return ResponseEntity.ok(response);
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
                // 订单完成时更新会员积分和累计消费
                updateMemberPointsAndSpending(order);
                // 添加出库记录
                createInventoryOutboundRecords(order);
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

    // 更新会员积分和累计消费（订单完成时调用）
    private void updateMemberPointsAndSpending(Order order) {
        User user = order.getUser();
        // 检查用户是否为会员
        if (user != null) {
            Member member = memberRepository.findByUser(user);
            if (member != null) {
                // 获取订单金额
                BigDecimal orderAmount = order.getTotalAmount();
                
                // 计算需要增加的积分 (1元 = 1积分)
                int pointsToAdd = orderAmount.intValue();
                
                // 更新会员积分
                int currentPoints = member.getPoints() != null ? member.getPoints() : 0;
                member.setPoints(currentPoints + pointsToAdd);
                
                // 更新会员累计消费
                double currentSpending = member.getTotalSpending() != null ? member.getTotalSpending() : 0.0;
                member.setTotalSpending(currentSpending + orderAmount.doubleValue());
                
                // 检查并更新会员等级
                updateMemberLevel(member);
                
                // 保存会员信息
                memberRepository.save(member);
                
                // 记录日志
                System.out.println("已为会员 " + member.getName() + 
                                  " (ID: " + member.getMemberId() + 
                                  ") 添加 " + pointsToAdd + 
                                  " 积分，订单金额: " + orderAmount);
            }
        }
    }

    // 根据累计消费和积分更新会员等级
    private void updateMemberLevel(Member member) {
        Double totalSpending = member.getTotalSpending();
        Integer points = member.getPoints();
        
        // 会员等级规则
        // bronze: 默认级别
        // silver: 累计消费 >= 1000 或 积分 >= 1000
        // gold: 累计消费 >= 5000 或 积分 >= 5000
        // platinum: 累计消费 >= 20000 或 积分 >= 20000
        
        String currentLevel = "bronze"; // 默认级别
        
        if (totalSpending >= 20000 || points >= 20000) {
            currentLevel = "platinum";
        } else if (totalSpending >= 5000 || points >= 5000) {
            currentLevel = "gold";
        } else if (totalSpending >= 1000 || points >= 1000) {
            currentLevel = "silver";
        }
        
        // 只在等级提升时更新，不降级
        if (shouldUpgradeLevel(member.getMemberLevel(), currentLevel)) {
            member.setMemberLevel(currentLevel);
            System.out.println("会员 " + member.getName() + 
                              " (ID: " + member.getMemberId() + 
                              ") 等级已提升至: " + currentLevel);
        }
    }

    // 判断是否应该升级会员等级
    private boolean shouldUpgradeLevel(String currentLevel, String newLevel) {
        // 如果当前等级为空，直接升级
        if (currentLevel == null) return true;
        
        // 会员等级权重映射
        java.util.Map<String, Integer> levelWeights = java.util.Map.of(
            "bronze", 1,
            "silver", 2,
            "gold", 3,
            "platinum", 4
        );
        
        // 获取当前等级和新等级的权重
        Integer currentWeight = levelWeights.getOrDefault(currentLevel.toLowerCase(), 0);
        Integer newWeight = levelWeights.getOrDefault(newLevel.toLowerCase(), 0);
        
        // 如果新等级权重大于当前等级权重，则升级
        return newWeight > currentWeight;
    }

    // 获取订单可评价商品
    @GetMapping("/{id}/reviewable-items")
    public ResponseEntity<ApiResponse<List<OrderItemResponse>>> getReviewableItems(@PathVariable Long id) {
        User user = getCurrentUser();
        
        // 检查订单是否存在
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        // 检查订单是否属于当前用户
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无法查看不属于您的订单");
        }
        
        // 检查订单状态是否为已发货或已完成
        if (!order.getStatus().equals("DELIVERING") && !order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("只有发货后的订单才能评价");
        }
        
        // 获取订单中未评价的商品
        List<OrderItemResponse> reviewableItems = order.getItems().stream()
            .filter(item -> !reviewRepository.existsByOrderIdAndProductId(order.getId(), item.getProduct().getId()))
            .map(this::convertToItemResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new ApiResponse<>(reviewableItems, 200, "获取成功"));
    }

    // 创建出库记录
    private void createInventoryOutboundRecords(Order order) {
        User currentUser = getCurrentUser();
        Date now = new Date();
        Store store = order.getStore();
        
        if (store == null) {
            System.out.println("警告: 订单没有关联门店，无法创建出库记录");
            return;
        }
        
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int quantity = item.getQuantity();
            
            // 创建出库记录
            InventoryRecord record = new InventoryRecord();
            record.setStore(store);
            record.setProduct(product);
            record.setOperator(currentUser);
            record.setRecordTime(now);
            record.setRecordType("outbound");
            record.setSourceType("order");
            record.setSourceId(order.getId());
            record.setQuantity(quantity);
            record.setRemark("订单号: " + order.getOrderNumber() + " 完成出库");
            
            // 保存出库记录
            try {
                inventoryRecordRepository.save(record);
                System.out.println("已为订单 " + order.getOrderNumber() + " 创建出库记录，商品: " + product.getName() + ", 数量: " + quantity);
            } catch (Exception e) {
                System.err.println("创建出库记录失败: " + e.getMessage());
                // 记录错误但不中断流程
                e.printStackTrace();
            }
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
        // 获取会员姓名和等级
        String memberName = null;
        String memberLevel = null;
        if (order.getUser() != null) {
            memberName = order.getUser().getName();
            // 如果用户是会员，获取会员等级
            Member member = memberRepository.findByUser(order.getUser());
            if (member != null) {
                memberLevel = member.getMemberLevel();
            }
        }
        
        // 获取店铺信息
        Long storeId = null;
        String storeName = null;
        if (order.getStore() != null) {
            storeId = order.getStore().getId();
            storeName = order.getStore().getName();
        }
        
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            order.getDeliveryMethod(),
            order.getPaymentMethod(),
            order.getStatus(),
            order.getCreateTime(),
            memberName,
            memberLevel,
            storeId,
            storeName,
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