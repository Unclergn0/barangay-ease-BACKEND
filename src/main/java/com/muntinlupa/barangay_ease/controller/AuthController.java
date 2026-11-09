package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.JwtService;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ResidentRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthController(ResidentRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest q) {
        if (q.email == null || q.password == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        Resident r = repo.findByEmail(q.email.trim()).orElse(null);
        if (r == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        boolean legacyPassword = !isBcrypt(r.getPassword());
        if (!matchesAndUpgrade(q.password, r.getPassword(), r::setPassword))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        if (legacyPassword) repo.save(r);
        AppUser user = new AppUser(r.getResidentId(), r.getEmail(), "RESIDENT", r.getBarangay());
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("residentID", r.getResidentId()); result.put("residentId", r.getResidentId()); result.put("id", r.getResidentId());
        result.put("firstName", r.getFirstName()); result.put("lastName", r.getLastName()); result.put("email", r.getEmail());
        result.put("barangay", r.getBarangay()); result.put("role", "RESIDENT"); result.put("token", jwt.generate(user));
        return ResponseEntity.ok(result);
    }

    private boolean isBcrypt(String value) { return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$")); }
    private boolean matchesAndUpgrade(String raw, String stored, java.util.function.Consumer<String> setter) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))
            return encoder.matches(raw, stored);
        if (raw.equals(stored)) { setter.accept(encoder.encode(raw)); return true; }
        return false;
    }
    public static class LoginRequest { public String email; public String password; }
}