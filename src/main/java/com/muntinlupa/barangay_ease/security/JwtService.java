package com.muntinlupa.barangay_ease.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final String JWT_TYPE = "JWT";

    private final byte[] secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:28800}") long expirationSeconds,
            ObjectMapper objectMapper
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret is required.");
        }

        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;

        if (this.secret.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 bytes."
            );
        }

        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero."
            );
        }
    }

    // ==========================================
    // GENERATE TOKEN
    // ==========================================

    public String generate(AppUser user) {
        try {
            validateUserForToken(user);

            long now = Instant.now().getEpochSecond();
            long expiration = Math.addExact(now, expirationSeconds);

            Map<String, Object> header = new HashMap<>();
            header.put("alg", JWT_ALGORITHM);
            header.put("typ", JWT_TYPE);

            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", user.email());
            claims.put("email", user.email());
            claims.put("uid", user.id());
            claims.put("role", user.role());
            claims.put("barangay", user.barangay());
            claims.put("barangay_id", user.barangayId());
            claims.put("iat", now);
            claims.put("exp", expiration);

            String encodedHeader = encode(
                    objectMapper.writeValueAsBytes(header)
            );

            String encodedClaims = encode(
                    objectMapper.writeValueAsBytes(claims)
            );

            String unsignedToken = encodedHeader + "." + encodedClaims;

            return unsignedToken + "." + sign(unsignedToken);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to generate JWT.",
                    e
            );
        }
    }

    private void validateUserForToken(AppUser user) {
        if (user == null
                || user.id() == null
                || user.id() <= 0
                || user.email() == null
                || user.email().isBlank()
                || user.role() == null
                || user.role().isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid user identity."
            );
        }

        if (!"ADMIN".equals(user.role())
                && !"RESIDENT".equals(user.role())) {

            throw new IllegalArgumentException(
                    "Invalid user role."
            );
        }

        if ("ADMIN".equals(user.role())) {
            if (user.barangayId() == null || user.barangayId() <= 0) {
                throw new IllegalArgumentException(
                        "Admin must have a valid barangay assignment."
                );
            }

            if (user.barangay() == null || user.barangay().isBlank()) {
                throw new IllegalArgumentException(
                        "Admin must have a valid barangay name."
                );
            }
        }
    }

    // ==========================================
    // PARSE AND VALIDATE TOKEN
    // ==========================================

    public AppUser parse(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Missing token.");
            }

            String[] parts = token.split("\\.", -1);

            if (parts.length != 3
                    || parts[0].isBlank()
                    || parts[1].isBlank()
                    || parts[2].isBlank()) {

                throw new IllegalArgumentException(
                        "Invalid JWT format."
                );
            }

            String unsignedToken = parts[0] + "." + parts[1];

            String expectedSignature = sign(unsignedToken);

            byte[] actualBytes = Base64.getUrlDecoder()
                    .decode(parts[2]);

            byte[] expectedBytes = Base64.getUrlDecoder()
                    .decode(expectedSignature);

            if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
                throw new IllegalArgumentException(
                        "Invalid JWT signature."
                );
            }

            // Validate header
            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]),
                    new TypeReference<Map<String, Object>>() {}
            );

            if (!JWT_ALGORITHM.equals(header.get("alg"))
                    || !JWT_TYPE.equals(header.get("typ"))) {

                throw new IllegalArgumentException(
                        "Unsupported JWT algorithm or type."
                );
            }

            // Read claims
            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {}
            );

            String subject = stringClaim(claims, "sub");
            String email = stringClaim(claims, "email");
            String role = stringClaim(claims, "role");

            Long userId = longClaim(claims, "uid");
            Long issuedAt = longClaim(claims, "iat");
            Long expiration = longClaim(claims, "exp");

            long now = Instant.now().getEpochSecond();

            // Validate identity claims
            if (!subject.equals(email)) {
                throw new IllegalArgumentException(
                        "JWT subject and email do not match."
                );
            }

            if (userId <= 0) {
                throw new IllegalArgumentException(
                        "Invalid user ID."
                );
            }

            if (!"ADMIN".equals(role)
                    && !"RESIDENT".equals(role)) {

                throw new IllegalArgumentException(
                        "Invalid user role."
                );
            }

            // Validate token timestamps
            if (issuedAt <= 0 || expiration <= 0) {
                throw new IllegalArgumentException(
                        "Invalid JWT timestamps."
                );
            }

            if (issuedAt > now + 60) {
                throw new IllegalArgumentException(
                        "JWT issued-at time is invalid."
                );
            }

            if (expiration <= issuedAt || now >= expiration) {
                throw new IllegalArgumentException(
                        "JWT has expired or has an invalid expiration."
                );
            }

            String barangay = optionalStringClaim(
                    claims,
                    "barangay"
            );

            Integer barangayId = integerClaim(
                    claims,
                    "barangay_id"
            );

            // Admin must always have a valid barangay assignment
            if ("ADMIN".equals(role)) {
                if (barangayId == null || barangayId <= 0) {
                    throw new IllegalArgumentException(
                            "Admin token has no valid barangay ID."
                    );
                }

                if (barangay == null || barangay.isBlank()) {
                    throw new IllegalArgumentException(
                            "Admin token has no valid barangay name."
                    );
                }
            }

            return new AppUser(
                    userId,
                    email,
                    role,
                    barangay,
                    barangayId
            );

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid or expired token.",
                    e
            );
        }
    }

    // ==========================================
    // SIGNATURE
    // ==========================================

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);

        mac.init(
                new SecretKeySpec(secret, ALGORITHM)
        );

        byte[] signature = mac.doFinal(
                data.getBytes(StandardCharsets.UTF_8)
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature);
    }

    // ==========================================
    // ENCODING
    // ==========================================

    private String encode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    // ==========================================
    // CLAIM VALIDATION
    // ==========================================

    private String stringClaim(
            Map<String, Object> claims,
            String key
    ) {
        Object value = claims.get(key);

        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing or invalid claim: " + key
            );
        }

        return text;
    }

    private String optionalStringClaim(
            Map<String, Object> claims,
            String key
    ) {
        Object value = claims.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(
                    "Invalid claim: " + key
            );
        }

        return text;
    }

    private Long longClaim(
            Map<String, Object> claims,
            String key
    ) {
        Object value = claims.get(key);

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "Invalid claim: " + key
            );
        }

        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid numeric claim: " + key,
                    e
            );
        }
    }

    private Integer integerClaim(
            Map<String, Object> claims,
            String key
    ) {
        Object value = claims.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "Invalid claim: " + key
            );
        }

        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid numeric claim: " + key,
                    e
            );
        }
    }
}