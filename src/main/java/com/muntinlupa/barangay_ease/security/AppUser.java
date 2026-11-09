
package com.muntinlupa.barangay_ease.security;

public record AppUser(
        Long id,
        String email,
        String role,
        String barangay,
        Integer barangayId
) {

    // Compatibility constructor for existing Integer IDs.
    public AppUser(
            Integer id,
            String email,
            String role,
            String barangay
    ) {
        this(
                id != null ? id.longValue() : null,
                email,
                role,
                barangay,
                null
        );
    }

    // Compatibility constructor for existing Long IDs.
    public AppUser(
            Long id,
            String email,
            String role,
            String barangay
    ) {
        this(id, email, role, barangay, null);
    }

    // Constructor for Integer IDs with barangay ID.
    public AppUser(
            Integer id,
            String email,
            String role,
            String barangay,
            Integer barangayId
    ) {
        this(
                id != null ? id.longValue() : null,
                email,
                role,
                barangay,
                barangayId
        );
    }
}
