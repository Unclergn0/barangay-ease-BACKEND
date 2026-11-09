
package com.muntinlupa.barangay_ease.dto;

import java.time.LocalDateTime;

public record ResidentAdminResponse(
        Integer residentId,
        String firstName,
        String middleName,
        String lastName,
        String contactNumber,
        String houseStreet,
        String barangay,
        Integer barangayId,
        String email,
        LocalDateTime createdAt
) {
}
