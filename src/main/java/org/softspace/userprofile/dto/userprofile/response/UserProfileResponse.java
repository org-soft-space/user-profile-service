package org.softspace.userprofile.dto.userprofile.response;

import org.softspace.userprofile.enums.ProfileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID guid,
        String name,
        String surname,
        String middleName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        ProfileStatus profileStatus,
        Instant updatedAt,
        Instant createdAt
) {
}
