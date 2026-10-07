package com.careerpilot.service;

import com.careerpilot.model.AdminUser;
import com.careerpilot.repository.AdminUserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminRepo;

    public AdminAuthService(AdminUserRepository adminRepo) {
        this.adminRepo = adminRepo;
    }

    @PostConstruct
    public void initDefaultAdmin() {
        if (adminRepo.findByUsername("admin").isEmpty()) {
            AdminUser admin = new AdminUser("admin", hashPassword("admin123"), "Lead Administrator");
            admin.setLastLogin(LocalDateTime.now());
            adminRepo.save(admin);
        }
    }

    public boolean authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) return false;
        Optional<AdminUser> opt = adminRepo.findByUsername(username.trim());
        if (opt.isPresent()) {
            AdminUser admin = opt.get();
            String hashed = hashPassword(rawPassword.trim());
            if (hashed.equals(admin.getPassword()) || rawPassword.equals(admin.getPassword())) {
                admin.setLastLogin(LocalDateTime.now());
                adminRepo.save(admin);
                return true;
            }
        }
        return false;
    }

    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) return false;
        AdminUser admin = adminRepo.findByUsername(username).orElse(null);
        if (admin != null && newPassword != null && newPassword.length() >= 5) {
            admin.setPassword(hashPassword(newPassword.trim()));
            adminRepo.save(admin);
            return true;
        }
        return false;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
