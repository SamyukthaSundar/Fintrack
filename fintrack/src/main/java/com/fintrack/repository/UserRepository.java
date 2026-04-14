package com.fintrack.repository;

import com.fintrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository
 * Owner: Saanvi Kakkar
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.fullName")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.isActive = false ORDER BY u.fullName")
    List<User> findAllInactiveUsers();

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findAllByIdIn(List<Long> ids);
}
