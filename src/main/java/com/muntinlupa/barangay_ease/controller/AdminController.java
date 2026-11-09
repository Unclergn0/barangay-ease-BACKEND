package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.model.Admin;
import com.muntinlupa.barangay_ease.model.Barangay;
import com.muntinlupa.barangay_ease.repository.AdminRepository;
import com.muntinlupa.barangay_ease.repository.BarangayRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminController.class);

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Barangay Staff",
            "Barangay Secretary",
            "Barangay Captain"
    );

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PIN_PATTERN =
            Pattern.compile("^\\d{6}$");

    // BCrypt supports a maximum of 72 UTF-8 bytes.
    private static final int BCRYPT_MAX_BYTES = 72;

    private final AdminRepository adminRepository;
    private final BarangayRepository barangayRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminController(
            AdminRepository adminRepository,
            BarangayRepository barangayRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.adminRepository = adminRepository;
        this.barangayRepository = barangayRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ==========================================
    // ADMIN REGISTRATION
    // PUBLIC REGISTRATION - PRESERVED
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(
            @RequestBody AdminRegistrationRequest request
    ) {
        if (request == null
                || isBlank(request.fullname())
                || isBlank(request.email())
                || isBlank(request.password())
                || isBlank(request.barangay())
                || isBlank(request.role())
                || isBlank(request.securityPin())) {

            return badRequest("Please complete all required fields.");
        }

        String fullname = request.fullname().trim();
        String email = normalizeEmail(request.email());
        String barangayName = request.barangay().trim();
        String password = request.password();
        String securityPin = request.securityPin().trim();

        // Full name validation
        if (fullname.length() < 2 || fullname.length() > 120) {
            return badRequest(
                    "Full name must be between 2 and 120 characters."
            );
        }

        // Email validation
        if (!isValidEmail(email)) {
            return badRequest("Please enter a valid email address.");
        }

        // Password validation
        if (password.length() < 8) {
            return badRequest(
                    "Password must contain at least 8 characters."
            );
        }

        if (exceedsBcryptLimit(password)) {
            return badRequest(
                    "Password is too long. Maximum supported length is 72 UTF-8 bytes."
            );
        }

        // Security PIN validation
        if (!PIN_PATTERN.matcher(securityPin).matches()) {
            return badRequest(
                    "Security PIN must contain exactly 6 digits."
            );
        }

        // Role validation
        String role = canonicalRole(request.role());

        if (role == null || !ALLOWED_ROLES.contains(role)) {
            return badRequest("Invalid admin role.");
        }

        try {
            if (adminRepository.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message",
                                "Email is already registered."
                        ));
            }

            // Always resolve barangay from the database.
            Barangay barangay = barangayRepository
                    .findByBarangayNameIgnoreCase(barangayName)
                    .orElse(null);

            if (barangay == null
                    || barangay.getBarangayId() == null
                    || barangay.getBarangayId() <= 0) {

                return badRequest("Invalid barangay.");
            }

            // One designated account per role, per barangay.
            // The barangay is resolved from the selected barangay name; no
            // authenticated admin can create an account for another barangay.
            if (adminRepository.existsByBarangayIdAndRoleIgnoreCase(
                    barangay.getBarangayId(), role)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message",
                                "The " + role + " account for Barangay "
                                        + barangay.getBarangayName()
                                        + " is already registered."));
            }

            Admin admin = new Admin();

            admin.setFullname(fullname);
            admin.setEmail(email);

            // Barangay ID comes from the database, not the request.
            admin.setBarangay(barangay.getBarangayName());
            admin.setBarangayId(barangay.getBarangayId());

            admin.setRole(role);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setSecurityPin(passwordEncoder.encode(securityPin));
            admin.setCreatedAt(LocalDateTime.now());

            Admin saved = adminRepository.save(admin);

            Map<String, Object> response = new HashMap<>();

            response.put("message", "Admin registration successful.");
            response.put("adminId", saved.getAdminId());
            response.put("fullname", saved.getFullname());
            response.put("email", saved.getEmail());
            response.put("barangay", saved.getBarangay());
            response.put("barangayId", saved.getBarangayId());
            response.put("role", saved.getRole());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(response);

        } catch (DataIntegrityViolationException e) {
            // Handles duplicate email caused by simultaneous requests.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Email is already registered or this barangay role is already occupied."
                    ));

        } catch (Exception e) {
            log.error("Admin registration failed.", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message",
                            "Unable to register admin. Please try again later."
                    ));
        }
    }

    // ==========================================
    // ADMIN LOGIN
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(
            @RequestBody Map<String, String> request
    ) {
        if (request == null) {
            return badRequest("Email and password are required.");
        }

        String email = normalizeEmail(request.get("email"));
        String password = request.get("password");

        if (isBlank(email) || isBlank(password)) {
            return badRequest("Email and password are required.");
        }

        if (!isValidEmail(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or password."
                    ));
        }

        Admin admin = adminRepository
                .findByEmail(email)
                .orElse(null);

        if (admin == null || !matchesAndUpgrade(password, admin)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or password."
                    ));
        }

        if (admin.getAdminId() == null
                || admin.getAdminId() <= 0
                || admin.getBarangayId() == null
                || admin.getBarangayId() <= 0
                || isBlank(admin.getBarangay())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Admin account has an invalid barangay assignment. Contact the system administrator."
                    ));
        }

        // Verify that the assigned barangay still exists.
        boolean barangayExists = barangayRepository
                .findByBarangayNameIgnoreCase(admin.getBarangay())
                .filter(b -> java.util.Objects.equals(b.getBarangayId(), admin.getBarangayId()))
                .isPresent();

        if (!barangayExists) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Admin barangay assignment is invalid. Contact the system administrator."
                    ));
        }

        AppUser user = new AppUser(
                admin.getAdminId(),
                admin.getEmail(),
                "ADMIN",
                admin.getBarangay(),
                admin.getBarangayId()
        );

        String token = jwtService.generate(user);

        Map<String, Object> response = new HashMap<>();

        response.put("token", token);
        response.put("adminId", admin.getAdminId());
        response.put("fullname", admin.getFullname());
        response.put("username", admin.getFullname());
        response.put("email", admin.getEmail());
        response.put("barangay", admin.getBarangay());
        response.put("barangayId", admin.getBarangayId());
        response.put("role", admin.getRole());

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // RESET ADMIN PASSWORD
    // ==========================================

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @RequestBody AdminResetRequest request
    ) {
        if (request == null
                || isBlank(request.email())
                || isBlank(request.securityPin())
                || isBlank(request.newPassword())) {

            return badRequest(
                    "Email, security PIN and new password are required."
            );
        }

        String email = normalizeEmail(request.email());
        String securityPin = request.securityPin().trim();
        String newPassword = request.newPassword();

        if (!isValidEmail(email)) {
            return badRequest("Please enter a valid email address.");
        }

        if (!PIN_PATTERN.matcher(securityPin).matches()) {
            return badRequest(
                    "Security PIN must contain exactly 6 digits."
            );
        }

        if (newPassword.length() < 8) {
            return badRequest(
                    "New password must contain at least 8 characters."
            );
        }

        if (exceedsBcryptLimit(newPassword)) {
            return badRequest(
                    "New password is too long. Maximum supported length is 72 UTF-8 bytes."
            );
        }

        Admin admin = adminRepository
                .findByEmail(email)
                .orElse(null);

        if (admin == null || !verifyPin(securityPin, admin)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or security PIN."
                    ));
        }

        admin.setPassword(
                passwordEncoder.encode(newPassword)
        );

        adminRepository.save(admin);

        return ResponseEntity.ok(Map.of(
                "message",
                "Password has been successfully reset."
        ));
    }

    // ==========================================
    // PASSWORD VERIFICATION
    // ==========================================

    private boolean matchesAndUpgrade(
            String rawPassword,
            Admin admin
    ) {
        String storedPassword = admin.getPassword();

        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcrypt(storedPassword)) {
            try {
                return passwordEncoder.matches(
                        rawPassword,
                        storedPassword
                );
            } catch (IllegalArgumentException e) {
                log.warn(
                        "Stored admin password hash is invalid for admin ID {}.",
                        admin.getAdminId()
                );
                return false;
            }
        }

        // Legacy plaintext password migration.
        if (MessageDigest.isEqual(
                rawPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8)
        )) {
            admin.setPassword(passwordEncoder.encode(rawPassword));
            adminRepository.save(admin);
            return true;
        }

        return false;
    }

    // ==========================================
    // SECURITY PIN VERIFICATION
    // ==========================================

    private boolean verifyPin(
            String rawPin,
            Admin admin
    ) {
        String storedPin = admin.getSecurityPin();

        if (storedPin == null || storedPin.isBlank()) {
            return false;
        }

        if (isBcrypt(storedPin)) {
            try {
                return passwordEncoder.matches(rawPin, storedPin);
            } catch (IllegalArgumentException e) {
                log.warn(
                        "Stored security PIN hash is invalid for admin ID {}.",
                        admin.getAdminId()
                );
                return false;
            }
        }

        // Legacy plaintext PIN migration.
        if (MessageDigest.isEqual(
                rawPin.getBytes(StandardCharsets.UTF_8),
                storedPin.getBytes(StandardCharsets.UTF_8)
        )) {
            admin.setSecurityPin(passwordEncoder.encode(rawPin));
            adminRepository.save(admin);
            return true;
        }

        return false;
    }

    private boolean isBcrypt(String value) {
        return value != null && (
                value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$")
        );
    }

    // ==========================================
    // ROLE VALIDATION
    // ==========================================

    private String canonicalRole(String role) {
        if (role == null) {
            return null;
        }

        return switch (role.trim().toLowerCase()) {
            case "barangay staff" -> "Barangay Staff";
            case "barangay secretary" -> "Barangay Secretary";
            case "barangay captain" -> "Barangay Captain";
            default -> null;
        };
    }

    // ==========================================
    // GENERAL VALIDATION
    // ==========================================

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeEmail(String email) {
        return email == null
                ? null
                : email.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {
        return email != null
                && email.length() <= 254
                && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length
                > BCRYPT_MAX_BYTES;
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record AdminRegistrationRequest(
            String fullname,
            String email,
            String barangay,
            String role,
            String password,
            String securityPin
    ) {}

    public record AdminResetRequest(
            String email,
            String securityPin,
            String newPassword
    ) {}
}