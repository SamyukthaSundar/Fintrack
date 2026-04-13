package com.fintrack.service;

import com.fintrack.dto.UserRegistrationDto;
import com.fintrack.model.User;

import java.util.List;
import java.util.Optional;

/**
 * UserService Interface
 * Owner: Saanvi Kakkar
 *
 * Design Principle: ISP — exposes only user-specific operations
 * Design Principle: DIP — controllers depend on this interface, not the implementation
 */
public interface UserService {
    User register(UserRegistrationDto dto);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    List<User> findAllActive();
    User getCurrentUser();
    User updateProfile(Long userId, String fullName, String email);
    void deactivateUser(Long userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
