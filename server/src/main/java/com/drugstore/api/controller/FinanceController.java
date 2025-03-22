package com.drugstore.api.controller;

import com.drugstore.api.model.*;
import com.drugstore.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreInventoryRepository storeInventoryRepository;

    /**
     * 获取财务统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date endDate) {
        
        Map<String, Object> statistics = new HashMap<>();

        // 计算今日时间范围
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date todayStart = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date todayEnd = calendar.getTime();

        // 计算昨日时间范围
        calendar.add(Calendar.DAY_OF_MONTH, -2);
        Date yesterdayStart = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date yesterdayEnd = calendar.getTime();

        // 查询今日订单总额
        List<Order> todayOrders = findOrdersByDateRangeAndStoreId(todayStart, todayEnd, storeId);
        BigDecimal todaySales = calculateTotalSales(todayOrders);
        int todayOrderCount = todayOrders.size();

        // 查询昨日订单总额
        List<Order> yesterdayOrders = findOrdersByDateRangeAndStoreId(yesterdayStart, yesterdayEnd, storeId);
        BigDecimal yesterdaySales = calculateTotalSales(yesterdayOrders);
        int yesterdayOrderCount = yesterdayOrders.size();

        // 计算增长率
        double salesGrowth = 0;
        if (yesterdaySales.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowth = (todaySales.doubleValue() - yesterdaySales.doubleValue()) / yesterdaySales.doubleValue() * 100;
        }

        double ordersGrowth = 0;
        if (yesterdayOrderCount > 0) {
            ordersGrowth = ((double) todayOrderCount - yesterdayOrderCount) / yesterdayOrderCount * 100;
        }

        // 计算客单价
        double averageOrder = 0;
        if (todayOrderCount > 0) {
            averageOrder = todaySales.doubleValue() / todayOrderCount;
        }

        // 计算毛利率
        double totalCost = calculateTotalCost(todayOrders);
        double grossProfitRate = 0;
        if (todaySales.doubleValue() > 0) {
            grossProfitRate = (todaySales.doubleValue() - totalCost) / todaySales.doubleValue() * 100;
        }

        // 组装统计结果
        statistics.put("todaySales", todaySales);
        statistics.put("salesGrowth", Math.round(salesGrowth * 100) / 100.0);
        statistics.put("todayOrders", todayOrderCount);
        statistics.put("ordersGrowth", Math.round(ordersGrowth * 100) / 100.0);
        statistics.put("averageOrder", Math.round(averageOrder * 100) / 100.0);
        statistics.put("grossProfitRate", Math.round(grossProfitRate * 100) / 100.0);

        return ResponseEntity.ok(new ApiResponse<>(statistics, 200, "获取财务统计数据成功"));
    }

    /**
     * 获取销售明细列表
     */
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getSalesList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date endDate) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 获取符合条件的订单
        Page<Order> orders = findOrdersPageByDateRangeAndStoreId(startDate, endDate, storeId, pageable);
        
        // 转换为前端需要的数据格式
        Page<Map<String, Object>> salesList = orders.map(order -> {
            Map<String, Object> saleItem = new HashMap<>();
            saleItem.put("id", order.getId());
            saleItem.put("orderNo", order.getOrderNumber());
            saleItem.put("storeName", order.getStore() != null ? order.getStore().getName() : "未知店铺");
            saleItem.put("amount", order.getTotalAmount());
            saleItem.put("paymentMethod", order.getPaymentMethod());
            
            // 计算毛利
            double cost = calculateOrderCost(order);
            double profit = order.getTotalAmount().doubleValue() - cost;
            saleItem.put("profit", profit);
            
            // 格式化日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            saleItem.put("createTime", order.getCreateTime() != null ? sdf.format(order.getCreateTime()) : "");
            
            return saleItem;
        });
        
        return ResponseEntity.ok(new ApiResponse<>(salesList, 200, "获取销售明细成功"));
    }

    /**
     * 获取销售趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesTrend(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date endDate) {
        
        Map<String, Object> trendData = new HashMap<>();
        List<String> timeLabels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Integer> ordersData = new ArrayList<>();

        // 如果没有指定日期范围，默认取当天
        if (startDate == null || endDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startDate = calendar.getTime();
            
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            endDate = calendar.getTime();
        }
        
        // 准备时间段数据
        for (int i = 0; i < 12; i++) {
            int hour = i * 2;
            timeLabels.add(String.format("%02d:00", hour));
            
            // 计算当前时间段的起止时间
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            startCal.set(Calendar.HOUR_OF_DAY, hour);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            Date periodStart = startCal.getTime();
            
            startCal.add(Calendar.HOUR_OF_DAY, 2);
            Date periodEnd = startCal.getTime();
            
            // 查询该时间段的订单
            List<Order> periodOrders = findOrdersByDateRangeAndStoreId(periodStart, periodEnd, storeId);
            
            // 计算销售额和订单数
            BigDecimal periodSales = calculateTotalSales(periodOrders);
            salesData.add(periodSales.doubleValue());
            ordersData.add(periodOrders.size());
        }
        
        trendData.put("timeLabels", timeLabels);
        trendData.put("salesData", salesData);
        trendData.put("ordersData", ordersData);
        
        return ResponseEntity.ok(new ApiResponse<>(trendData, 200, "获取销售趋势数据成功"));
    }

    /**
     * 获取支付方式占比数据
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentMethodStats(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date endDate) {
        
        // 查询符合条件的订单
        List<Order> orders = findOrdersByDateRangeAndStoreId(startDate, endDate, storeId);
        
        // 统计各种支付方式的金额
        Map<String, BigDecimal> paymentAmounts = new HashMap<>();
        for (Order order : orders) {
            String method = order.getPaymentMethod();
            if (method == null) {
                method = "other";
            }
            
            BigDecimal currentAmount = paymentAmounts.getOrDefault(method, BigDecimal.ZERO);
            paymentAmounts.put(method, currentAmount.add(order.getTotalAmount()));
        }
        
        // 转换为前端需要的格式
        List<Map<String, Object>> paymentStats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : paymentAmounts.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            String methodName = getPaymentMethodName(entry.getKey());
            stat.put("value", entry.getValue());
            stat.put("name", methodName);
            paymentStats.add(stat);
        }
        
        return ResponseEntity.ok(new ApiResponse<>(paymentStats, 200, "获取支付方式统计数据成功"));
    }

    /**
     * 获取热销商品TOP10
     */
    @GetMapping("/hot-products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHotProducts(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-DD") Date endDate) {
        
        // 查询符合条件的订单
        List<Order> orders = findOrdersByDateRangeAndStoreId(startDate, endDate, storeId);
        
        // 统计各商品的销售额
        Map<Product, BigDecimal> productSalesMap = new HashMap<>();
        for (Order order : orders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Product product = item.getProduct();
                    if (product != null) {
                        BigDecimal itemTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                        BigDecimal currentSales = productSalesMap.getOrDefault(product, BigDecimal.ZERO);
                        productSalesMap.put(product, currentSales.add(itemTotal));
                    }
                }
            }
        }
        
        // 按销售额排序并取TOP10
        List<Map.Entry<Product, BigDecimal>> sortedProducts = new ArrayList<>(productSalesMap.entrySet());
        sortedProducts.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        // 转换为前端需要的格式
        List<Map<String, Object>> hotProducts = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Product, BigDecimal> entry : sortedProducts) {
            if (count >= 10) break;
            
            Map<String, Object> productData = new HashMap<>();
            productData.put("name", entry.getKey().getName());
            productData.put("sales", entry.getValue().doubleValue());
            hotProducts.add(productData);
            count++;
        }
        
        return ResponseEntity.ok(new ApiResponse<>(hotProducts, 200, "获取热销商品数据成功"));
    }

    /**
     * 根据日期范围和店铺ID查询订单
     */
    private List<Order> findOrdersByDateRangeAndStoreId(Date startDate, Date endDate, Long storeId) {
        List<Order> orders;
        
        if (storeId != null) {
            // 查询特定店铺的订单
            if (startDate != null && endDate != null) {
                orders = orderRepository.findByStoreIdAndCreateTimeBetweenAndStatus(
                        storeId, startDate, endDate, "COMPLETED");
            } else {
                orders = orderRepository.findByStoreIdAndStatus(storeId, "COMPLETED");
            }
        } else {
            // 查询所有店铺的订单
            if (startDate != null && endDate != null) {
                orders = orderRepository.findByCreateTimeBetweenAndStatus(startDate, endDate, "COMPLETED");
            } else {
                orders = orderRepository.findByStatus("COMPLETED");
            }
        }
        
        return orders != null ? orders : new ArrayList<>();
    }

    /**
     * 根据日期范围和店铺ID分页查询订单
     */
    private Page<Order> findOrdersPageByDateRangeAndStoreId(Date startDate, Date endDate, Long storeId, Pageable pageable) {
        Page<Order> ordersPage;
        
        if (storeId != null) {
            // 查询特定店铺的订单
            if (startDate != null && endDate != null) {
                ordersPage = orderRepository.findByStoreIdAndCreateTimeBetweenAndStatus(
                        storeId, startDate, endDate, "COMPLETED", pageable);
            } else {
                ordersPage = orderRepository.findByStoreIdAndStatus(storeId, "COMPLETED", pageable);
            }
        } else {
            // 查询所有店铺的订单
            if (startDate != null && endDate != null) {
                ordersPage = orderRepository.findByCreateTimeBetweenAndStatus(startDate, endDate, "COMPLETED", pageable);
            } else {
                ordersPage = orderRepository.findByStatus("COMPLETED", pageable);
            }
        }
        
        return ordersPage;
    }

    /**
     * 计算订单的总销售额
     */
    private BigDecimal calculateTotalSales(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算订单的总成本
     */
    private double calculateTotalCost(List<Order> orders) {
        return orders.stream().mapToDouble(this::calculateOrderCost).sum();
    }

    /**
     * 计算单个订单的成本
     */
    private double calculateOrderCost(Order order) {
        double totalCost = 0;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    // 获取商品成本
                    double cost = product.getCost() != null ? product.getCost().doubleValue() : 0;
                    totalCost += cost * item.getQuantity();
                }
            }
        }
        return totalCost;
    }

    /**
     * 获取支付方式的显示名称
     */
    private String getPaymentMethodName(String method) {
        switch (method) {
            case "wechat":
                return "微信支付";
            case "alipay":
                return "支付宝";
            case "cash":
                return "现金";
            case "card":
                return "银行卡";
            default:
                return "其他方式";
        }
    }
} 