package com.drugstore.api.repository;

import com.drugstore.api.model.Member;
import com.drugstore.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByUser(User user);
    
    Member findByMemberId(String memberId);
    
    Page<Member> findAll(Pageable pageable);
    
    Page<Member> findByNameContainingOrPhoneNumberContaining(String name, String phoneNumber, Pageable pageable);
    
    Page<Member> findByStatus(String status, Pageable pageable);
    
    Page<Member> findByStatusAndNameContainingOrPhoneNumberContaining(
        String status, String name, String phoneNumber, Pageable pageable);
    
    long count();
} 