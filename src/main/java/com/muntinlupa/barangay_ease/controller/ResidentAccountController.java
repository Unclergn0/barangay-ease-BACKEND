package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.CurrentUser;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/resident")
public class ResidentAccountController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private final ResidentRepository repo;
    private final PasswordEncoder encoder;

    public ResidentAccountController(ResidentRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> profile(@RequestBody(required = false) Profile q) {
        AppUser user = CurrentUser.get();
        if (q == null) return ResponseEntity.badRequest().body(Map.of("message", "Profile details are required."));
        if (!"RESIDENT".equals(user.role()))
            return ResponseEntity.status(403).body(Map.of("message", "Resident access only."));
        Resident r = repo.findById(Math.toIntExact(user.id())).orElse(null);
        if (r == null)
            return ResponseEntity.notFound().build();
        if (q.firstName != null && !q.firstName.isBlank()) {
            if (q.firstName.trim().length() > 100) return ResponseEntity.badRequest().body(Map.of("message", "First name is too long."));
            r.setFirstName(q.firstName.trim());
        }
        if (q.lastName != null && !q.lastName.isBlank()) {
            if (q.lastName.trim().length() > 100) return ResponseEntity.badRequest().body(Map.of("message", "Last name is too long."));
            r.setLastName(q.lastName.trim());
        }
        if (q.email != null && !q.email.isBlank()) {
            String email = q.email.trim().toLowerCase(Locale.ROOT);
            if (email.length() > 150 || !EMAIL_PATTERN.matcher(email).matches())
                return ResponseEntity.badRequest().body(Map.of("message", "Please enter a valid email address."));
            Optional<Resident> other = repo.findByEmail(email);
            if (other.isPresent() && !other.get().getResidentId().equals(Math.toIntExact(user.id())))
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use."));
            r.setEmail(email);
        }
        repo.save(r);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }

    @PutMapping("/password")
    public ResponseEntity<?> password(@RequestBody(required = false) Password q) {
        AppUser user = CurrentUser.get();
        if (q == null) return ResponseEntity.badRequest().body(Map.of("message", "Password details are required."));
        if (!"RESIDENT".equals(user.role()))
            return ResponseEntity.status(403).body(Map.of("message", "Resident access only."));
        Resident r = repo.findById(Math.toIntExact(user.id())).orElse(null);
        if (r == null)
            return ResponseEntity.notFound().build();
        if (q.currentPassword == null || !passwordMatches(q.currentPassword, r.getPassword()))
            return ResponseEntity.status(403).body(Map.of("message", "Current password is incorrect."));
        if (q.newPassword == null || q.newPassword.length() < 8
                || q.newPassword.getBytes(StandardCharsets.UTF_8).length > 72)
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 8 characters and no more than 72 UTF-8 bytes."));
        r.setPassword(encoder.encode(q.newPassword));
        repo.save(r);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    private boolean passwordMatches(String raw, String stored) {
        if (stored == null)
            return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))
            return encoder.matches(raw, stored);
        return raw.equals(stored);
    }

    public static class Profile {
        public String firstName;
        public String lastName;
        public String email;
    }

    public static class Password {
        public String currentPassword;
        public String newPassword;
    }
}