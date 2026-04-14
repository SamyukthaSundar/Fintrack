package com.fintrack.service.impl;

import com.fintrack.dto.UserRegistrationDto;
import com.fintrack.model.User;
import com.fintrack.repository.UserRepository;
import com.fintrack.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserServiceImpl — Owner: Saanvi Kakkar
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(UserRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + dto.getUsername());
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + dto.getEmail());
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .role(User.Role.USER)
                .isActive(true)
                .build();
        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getUsername());
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) { return userRepository.findByUsername(username); }

    @Override @Transactional(readOnly = true)
    public Optional<User> findById(Long id) { return userRepository.findById(id); }

    @Override @Transactional(readOnly = true)
    public List<User> findAllActive() { return userRepository.findAllActiveUsers(); }

    @Override @Transactional(readOnly = true)
    public List<User> findAllInactive() { return userRepository.findAllInactiveUsers(); }

    @Override @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    @Override
    public User updateProfile(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFullName(fullName);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User {} deactivated.", user.getUsername());
    }

    @Override
    public void reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User {} reactivated.", user.getUsername());
    }

    @Override @Transactional(readOnly = true)
    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }

    @Override @Transactional(readOnly = true)
    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email); }
}
