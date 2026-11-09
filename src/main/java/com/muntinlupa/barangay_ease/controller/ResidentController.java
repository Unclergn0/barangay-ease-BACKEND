package com.muntinlupa.barangay_ease.controller;

import com.muntinlupa.barangay_ease.dto.ResidentAdminResponse;
import com.muntinlupa.barangay_ease.model.Barangay;
import com.muntinlupa.barangay_ease.model.Resident;
import com.muntinlupa.barangay_ease.repository.BarangayRepository;
import com.muntinlupa.barangay_ease.repository.ResidentRepository;
import com.muntinlupa.barangay_ease.security.AppUser;
import com.muntinlupa.barangay_ease.security.JwtService;
import com.muntinlupa.barangay_ease.service.ResidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\- ]{7,15}$");

    private final ResidentRepository residentRepository;
    private final BarangayRepository barangayRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ResidentService residentService;

    public ResidentController(
            ResidentRepository residentRepository,
            BarangayRepository barangayRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ResidentService residentService) {
        this.residentRepository = residentRepository;
        this.barangayRepository = barangayRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.residentService = residentService;
    }

    /** Admins can retrieve residents assigned to their own barangay only. */
    @GetMapping
    public ResponseEntity<?> getResidents(@AuthenticationPrincipal AppUser admin) {
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.role()) || admin.barangayId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Admin authentication and barangay assignment are required."));
        }
        List<ResidentAdminResponse> residents = residentService.getResidentsByBarangay(admin.barangayId());
        return ResponseEntity.ok(residents);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerResident(@RequestBody(required = false) Resident resident) {
        if (resident == null) return badRequest("Registration details are required.");

        String firstName = clean(resident.getFirstName());
        String lastName = clean(resident.getLastName());
        String middleName = clean(resident.getMiddleName());
        String contact = clean(resident.getContactNumber());
        String address = clean(resident.getHouseStreet());
        String email = resident.getEmail() == null ? "" : resident.getEmail().trim().toLowerCase(Locale.ROOT);
        String barangayName = clean(resident.getBarangay());
        String password = resident.getPassword();
        String pin = resident.getSecurityPin();

        if (firstName.isBlank() || lastName.isBlank() || contact.isBlank() || address.isBlank()
                || email.isBlank() || barangayName.isBlank() || password == null || pin == null) {
            return badRequest("Please complete all required registration fields.");
        }
        if (firstName.length() > 100 || lastName.length() > 100 || middleName.length() > 100
                || address.length() > 255 || email.length() > 150) {
            return badRequest("One or more fields exceed the allowed length.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) return badRequest("Please enter a valid email address.");
        if (!PHONE_PATTERN.matcher(contact).matches()) return badRequest("Please enter a valid contact number.");
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return badRequest("Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }
        if (!PIN_PATTERN.matcher(pin.trim()).matches()) return badRequest("Security PIN must contain exactly 6 digits.");

        Barangay barangay = barangayRepository.findByBarangayNameIgnoreCase(barangayName).orElse(null);
        if (barangay == null || barangay.getBarangayId() == null) return badRequest("Please select a valid barangay.");
        if (residentRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email is already registered."));
        }
        if (residentRepository.findByContactNumber(contact).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Contact number is already registered."));
        }

        resident.setFirstName(firstName);
        resident.setMiddleName(middleName.isBlank() ? null : middleName);
        resident.setLastName(lastName);
        resident.setContactNumber(contact);
        resident.setHouseStreet(address);
        resident.setEmail(email);
        // The database is authoritative. Never accept a client-supplied barangay ID.
        resident.setBarangay(barangay.getBarangayName());
        resident.setBarangayId(barangay.getBarangayId());
        resident.setPassword(passwordEncoder.encode(password));
        resident.setSecurityPin(passwordEncoder.encode(pin.trim()));
        resident.setCreatedAt(LocalDateTime.now());

        Resident saved = residentRepository.save(resident);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Resident registration successful.",
                "residentId", saved.getResidentId()));
    }

    /** Backward-compatible resident login endpoint. */
    @PostMapping("/login")
    public ResponseEntity<?> loginResident(@RequestBody(required = false) LoginRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            return badRequest("Email and password are required.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Resident resident = residentRepository.findByEmail(email).orElse(null);
        if (resident == null || !matchesAndUpgrade(request.password(), resident.getPassword(), resident::setPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        }
        residentRepository.save(resident);
        String token = jwtService.generate(new AppUser(
                resident.getResidentId(), resident.getEmail(), "RESIDENT", resident.getBarangay()));
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", "RESIDENT");
        response.put("residentId", resident.getResidentId());
        response.put("residentID", resident.getResidentId());
        response.put("firstName", resident.getFirstName());
        response.put("lastName", resident.getLastName());
        response.put("email", resident.getEmail());
        response.put("barangay", resident.getBarangay());
        response.put("barangayId", resident.getBarangayId());
        return ResponseEntity.ok(response);
    }

    /** Password reset requires the registered six-digit security PIN. */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody(required = false) ResetPasswordRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.securityPin()) || isBlank(request.newPassword())) {
            return badRequest("Email, security PIN and new password are required.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String pin = request.securityPin().trim();
        String newPassword = request.newPassword();
        if (!EMAIL_PATTERN.matcher(email).matches()) return badRequest("Please enter a valid email address.");
        if (!PIN_PATTERN.matcher(pin).matches()) return badRequest("Security PIN must contain exactly 6 digits.");
        if (newPassword.length() < 8 || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            return badRequest("New password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }

        Resident resident = residentRepository.findByEmail(email).orElse(null);
        if (resident == null || !matchesAndUpgrade(pin, resident.getSecurityPin(), resident::setSecurityPin)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or security PIN."));
        }
        resident.setPassword(passwordEncoder.encode(newPassword));
        residentRepository.save(resident);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
    }

    private boolean matchesAndUpgrade(String raw, String stored, java.util.function.Consumer<String> setter) {
        if (stored == null || stored.isBlank()) return false;
        if (isBcrypt(stored)) {
            try { return passwordEncoder.matches(raw, stored); }
            catch (IllegalArgumentException ex) { return false; }
        }
        if (MessageDigest.isEqual(raw.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8))) {
            setter.accept(passwordEncoder.encode(raw));
            return true;
        }
        return false;
    }

    private boolean isBcrypt(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    public record LoginRequest(String email, String password) {}
    public record ResetPasswordRequest(String email, String securityPin, String newPassword) {}
}
