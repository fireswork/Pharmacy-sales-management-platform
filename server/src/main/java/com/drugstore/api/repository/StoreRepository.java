package com.drugstore.api.repository;

import com.drugstore.api.model.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByStatus(String status);
    
    Store findByCode(String code);
    
    Page<Store> findByNameContainingOrCodeContaining(String name, String code, Pageable pageable);
    
    Page<Store> findByStatus(String status, Pageable pageable);
    
    Page<Store> findByStatusAndNameContainingOrCodeContaining(
        String status, String name, String code, Pageable pageable);
} 