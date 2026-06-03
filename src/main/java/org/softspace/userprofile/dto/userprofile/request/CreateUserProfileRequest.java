package org.softspace.userprofile.dto.userprofile.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.softspace.userprofile.controller.pattern.RequestPatterns;

import java.time.LocalDate;

public record CreateUserProfileRequest(
        @Size(max = 100)
        @NotBlank
        String name,

        @Size(max = 100)
        @NotBlank
        String surname,

        @Size(max = 100)
        String middleName,

        @PastOrPresent(message = "Date of birth can not be in the future.")
        LocalDate dateOfBirth,

        @Email
        @NotBlank
        @Size(max = 255)
        String email,

        @Pattern(
                regexp = RequestPatterns.phonePattern,
                message = "Phone must be in international format, for example: +79991234567"
        )
        String phone
) {
}
