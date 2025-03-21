package com.drugstore.api.repository;

import com.drugstore.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByNameContaining(String name);
    List<Product> findByCategory(String category);
    List<Product> findBySupplier(String supplier);
    Optional<Product> findByCode(String code);

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET status = :status WHERE id = :id", nativeQuery = true)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET `usage` = :usage WHERE id = :id", nativeQuery = true)
    void updateUsage(@Param("id") Long id, @Param("usage") String usage);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO products (`name`, `code`, `category`, `specification`, `manufacturer`, `supplier`, `approval_number`, `retail_price`, `cost_price`, `image`, `description`, `usage`, `status`) VALUES (:name, :code, :category, :specification, :manufacturer, :supplier, :approvalNumber, :retailPrice, :costPrice, :image, :description, :usage, :status)", nativeQuery = true)
    void insertProduct(@Param("name") String name, @Param("code") String code, @Param("category") String category, @Param("specification") String specification, @Param("manufacturer") String manufacturer, @Param("supplier") String supplier, @Param("approvalNumber") String approvalNumber, @Param("retailPrice") BigDecimal retailPrice, @Param("costPrice") BigDecimal costPrice, @Param("image") String image, @Param("description") String description, @Param("usage") String usage, @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET price = :price WHERE id = :id", nativeQuery = true)
    void updatePrice(@Param("id") Long id, @Param("price") BigDecimal price);
} 