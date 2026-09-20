package com.example.governmentsubsidy.service.admin;

import com.example.governmentsubsidy.entity.Office;
import com.example.governmentsubsidy.entity.Role;
import com.example.governmentsubsidy.entity.User;
import com.example.governmentsubsidy.repository.OfficeRepository;
import com.example.governmentsubsidy.repository.RoleRepository;
import com.example.governmentsubsidy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            OfficeRepository officeRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.officeRepository = officeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(String username, String rawPassword, String fullName, String email, String phone, Set<Role> roles, Long officeId) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoles(roles);
        if (officeId != null) {
            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new IllegalArgumentException("Office not found with id " + officeId));
            user.setOffice(office);
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, String phone, Set<Role> roles, Long officeId, Boolean enabled) {
        return userRepository.findById(id).map(user -> {
            if (fullName != null) user.setFullName(fullName);
            if (email != null) user.setEmail(email);
            if (phone != null) user.setPhone(phone);
            if (roles != null) user.setRoles(roles);
            if (officeId != null) {
                Office office = officeRepository.findById(officeId)
                        .orElseThrow(() -> new IllegalArgumentException("Office not found with id " + officeId));
                user.setOffice(office);
            }
            if (enabled != null) user.setEnabled(enabled);
            return userRepository.save(user);
        }).orElseThrow(() -> new IllegalArgumentException("User not found with id " + id));
    }

    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
    }
}
