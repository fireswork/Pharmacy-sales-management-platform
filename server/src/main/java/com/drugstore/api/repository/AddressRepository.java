package com.drugstore.api.repository;

import com.drugstore.api.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    
    Address findByUserIdAndIsDefaultTrue(Long userId);
} 