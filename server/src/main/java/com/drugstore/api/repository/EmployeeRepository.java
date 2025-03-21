package com.drugstore.api.repository;

import com.drugstore.api.model.Employee;
import com.drugstore.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Employee findById(String id);
    
    Employee findByUser(User user);
    
    Page<Employee> findAll(Pageable pageable);
    
    Page<Employee> findByNameContainingOrPhoneNumberContaining(String name, String phone, Pageable pageable);
    
    Page<Employee> findByStatus(String status, Pageable pageable);
    
    Page<Employee> findByStatusAndNameContainingOrPhoneNumberContaining(
        String status, String name, String phone, Pageable pageable);
    
    Page<Employee> findByStoreId(Long storeId, Pageable pageable);
    
    Page<Employee> findByStoreIdAndStatus(Long storeId, String status, Pageable pageable);
    
    Page<Employee> findByStoreIdAndNameContainingOrPhoneNumberContaining(
        Long storeId, String name, String phone, Pageable pageable);
    
    Page<Employee> findByStoreIdAndStatusAndNameContainingOrPhoneNumberContaining(
        Long storeId, String status, String name, String phone, Pageable pageable);
    
    @Query("SELECT COUNT(e) FROM Employee e")
    long countEmployees();

    Optional<Employee> findTopByOrderByCodeDesc();
} 