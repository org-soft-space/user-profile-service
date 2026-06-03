package org.softspace.userprofile.service.validator;

import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class UserProfileValidator {

    public void validUpdateUserProfileRequest(UpdateUserProfileRequest updateUserProfileRequest) {
        validateParameter(updateUserProfileRequest.name(),
                name -> name == null || name.isBlank(),
                "name"
        );
        validateParameter(updateUserProfileRequest.surname(),
                surname -> surname == null || surname.isBlank(),
                "surname"
        );
        validateParameter(updateUserProfileRequest.dateOfBirth(),
                dateOfBirth -> {
                    LocalDate now = LocalDate.now();
                    return (dateOfBirth == null) || dateOfBirth.isAfter(now);
                },
                "date of birth"
        );
        validateParameter(updateUserProfileRequest.profileStatus(),
                profileStatus -> {
                    return profileStatus == null ? true : false;
                },
                "profile status"
        );
    }

    private <T> void validateParameter(
            JsonNullable<T> value,
            Predicate<T> predicate,
            String fieldName
    ) {
        if (value == null || !value.isPresent()) {
            return;
        }
        boolean result = predicate.test(value.orElse(null));
        if (result) {
            throw new ValidationException("Validation error of field: " + fieldName, Map.of("fieldName", fieldName));
        }
    }
}
