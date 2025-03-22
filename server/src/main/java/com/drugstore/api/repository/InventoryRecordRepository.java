package com.drugstore.api.repository;

import com.drugstore.api.model.InventoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface InventoryRecordRepository extends JpaRepository<InventoryRecord, Long> {
    
    // 根据门店ID和产品ID查询记录
    List<InventoryRecord> findByStoreIdAndProductId(Long storeId, Long productId);
    
    // 分页查询门店库存记录
    Page<InventoryRecord> findByStoreId(Long storeId, Pageable pageable);
    
    // 按类型查询门店库存记录
    Page<InventoryRecord> findByStoreIdAndRecordType(Long storeId, String recordType, Pageable pageable);
    
    // 按关键字查询门店库存记录（产品名称或编码）
    @Query("SELECT r FROM InventoryRecord r WHERE r.store.id = ?1 AND (r.product.name LIKE %?2% OR r.product.code LIKE %?2%)")
    Page<InventoryRecord> findByStoreIdAndKeyword(Long storeId, String keyword, Pageable pageable);
    
    // 按时间范围查询门店库存记录
    Page<InventoryRecord> findByStoreIdAndRecordTimeBetween(Long storeId, Date startDate, Date endDate, Pageable pageable);
    
    // 按类型和关键字查询
    @Query("SELECT r FROM InventoryRecord r WHERE r.store.id = ?1 AND r.recordType = ?2 AND (r.product.name LIKE %?3% OR r.product.code LIKE %?3%)")
    Page<InventoryRecord> findByStoreIdAndRecordTypeAndKeyword(Long storeId, String recordType, String keyword, Pageable pageable);
    
    // 按类型和时间范围查询
    Page<InventoryRecord> findByStoreIdAndRecordTypeAndRecordTimeBetween(Long storeId, String recordType, Date startDate, Date endDate, Pageable pageable);
    
    // 按关键字和时间范围查询
    @Query("SELECT r FROM InventoryRecord r WHERE r.store.id = ?1 AND (r.product.name LIKE %?2% OR r.product.code LIKE %?2%) AND r.recordTime BETWEEN ?3 AND ?4")
    Page<InventoryRecord> findByStoreIdAndKeywordAndRecordTimeBetween(Long storeId, String keyword, Date startDate, Date endDate, Pageable pageable);
    
    // 按类型、关键字和时间范围查询
    @Query("SELECT r FROM InventoryRecord r WHERE r.store.id = ?1 AND r.recordType = ?2 AND (r.product.name LIKE %?3% OR r.product.code LIKE %?3%) AND r.recordTime BETWEEN ?4 AND ?5")
    Page<InventoryRecord> findByStoreIdAndRecordTypeAndKeywordAndRecordTimeBetween(Long storeId, String recordType, String keyword, Date startDate, Date endDate, Pageable pageable);
} 