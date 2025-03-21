package com.drugstore.api.repository;

import com.drugstore.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    User findByUsername(String username);
    long countByRole(String role);
    Optional<User> findTopByUsernameStartingWithOrderByUsernameDesc(String prefix);
} 