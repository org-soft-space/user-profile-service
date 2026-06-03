package org.softspace.userprofile.service.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.DtoTestBuilder;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.enums.ProfileStatus;

import org.softspace.userprofile.exception.ValidationException;

import java.time.LocalDate;
import java.util.stream.Stream;


class ValidatorTest {

    private final UserProfileValidator userProfileValidator = new UserProfileValidator();

    @Test
    void nullableUpdateUserProfileSuccessfullyTest() {

        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.of("TestName"),
                JsonNullable.of("TestSurname"),
                JsonNullable.of(DtoTestBuilder.MIDDLE_NAME),
                JsonNullable.of(LocalDate.of(1990, 10, 1)),
                JsonNullable.of(DtoTestBuilder.PHONE),
                JsonNullable.of(ProfileStatus.INACTIVE)
        );

        // Then
        Assertions.assertDoesNotThrow(
                () -> userProfileValidator.validUpdateUserProfileRequest(updateUserProfile)
        );
    }

    @ParameterizedTest(name = "name={0}, surname={1}, dateOfBirth={2}, profileStatus={3}")
    @MethodSource("userProfileVariants")
    void notNullableFieldsUpdateUserProfileNegativeTest(
            String name,
            String surname,
            LocalDate dateOfBirth,
            ProfileStatus profileStatus
    ) {
        // Given
        UpdateUserProfileRequest updateUserProfileRequest = new UpdateUserProfileRequest(
                JsonNullable.of(name),
                JsonNullable.of(surname),
                JsonNullable.of(DtoTestBuilder.MIDDLE_NAME),
                JsonNullable.of(dateOfBirth),
                JsonNullable.of(DtoTestBuilder.PHONE),
                JsonNullable.of(profileStatus)
                );

        // Then
        Assertions.assertThrows(
                ValidationException.class,
                () -> userProfileValidator.validUpdateUserProfileRequest(updateUserProfileRequest)
        );
    }

    private static Stream<Arguments> userProfileVariants() {
        return Stream.of(
                Arguments.of(null, "Surname", LocalDate.now(), ProfileStatus.ACTIVE),
                Arguments.of("Name", null, LocalDate.now(), ProfileStatus.INACTIVE),
                Arguments.of("Name", "Surname", null, ProfileStatus.BLOCKED),
                Arguments.of("Name", "Surname", LocalDate.now(), null)
        );
    }
}
