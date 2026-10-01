package com.example.gatherly.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.gatherly.model.Role;
import com.example.gatherly.model.User;
import com.example.gatherly.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerAttendeeOrOrganizer(String fullName, String email, String rawPassword, Role role) {
        if (role != Role.ATTENDEE && role != Role.ORGANIZER) {
            throw new BusinessRuleException("Invalid role selected.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword)); // HASH, never store raw
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public void setEnabled(Long userId, boolean enabled) {
        User user = getById(userId);
        user.setEnabled(enabled);
        userRepository.save(user);
    }
}