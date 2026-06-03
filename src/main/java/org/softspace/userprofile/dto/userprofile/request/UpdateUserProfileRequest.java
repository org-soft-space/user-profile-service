package org.softspace.userprofile.dto.userprofile.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.controller.pattern.RequestPatterns;
import org.softspace.userprofile.enums.ProfileStatus;

import java.time.LocalDate;

public record UpdateUserProfileRequest(

        JsonNullable<@Size(max = 100) String> name,

        JsonNullable<@Size(max = 100) String> surname,

        // TODO: JsonNullable<String>
        //  JacksonDataBindNullable
        JsonNullable<@Size(max = 100) String> middleName,

        JsonNullable<LocalDate> dateOfBirth,

        JsonNullable<@Size(max = 20) @Pattern(
                regexp = RequestPatterns.phonePattern,
                message = "Phone must be in international format, for example: +79991234567"
        )
        String> phone,

        JsonNullable<ProfileStatus> profileStatus
) {
}
