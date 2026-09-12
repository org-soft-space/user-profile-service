package org.softspace.userprofile;

import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.response.AllUserProfilesResponse;
import org.softspace.userprofile.dto.userprofile.response.UserProfileResponse;
import org.softspace.userprofile.enums.ProfileStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DtoTestBuilder {

    public static final String NAME = "Petr";
    public static final String MIDDLE_NAME = "Petrovich";
    public static final String SURNAME = "Petrov";
    public static final UUID USER_GUID = UUID.randomUUID();
    public static final ProfileStatus DEFAULT_PROFILE_STATUS = ProfileStatus.ACTIVE;
    public static final String PHONE = "+79991234567";
    public static final String EMAIL = "user@test.com";
    public static final LocalDate DATE_OF_BIRTH = LocalDate.of(1995, 5, 20);
    public static final Instant UPDATED = Instant.parse("2011-12-03T10:15:30Z");

    public static final String OTHER_NAME = "Ivan";
    public static final String OTHER_MIDDLE_NAME = "Ivanovich";
    public static final String OTHER_SURNAME = "Ivanov";
    public static final ProfileStatus OTHER_PROFILE_STATUS = ProfileStatus.INACTIVE;
    public static final String OTHER_PHONE = "+79997654321";
    public static final String OTHER_EMAIL = "otheruser@test.com";
    public static final LocalDate OTHER_DATE_OF_BIRTH = LocalDate.of(1985, 10, 10);

    public static CreateUserProfileRequest getCreateUserProfileRequest() {
        return new CreateUserProfileRequest(
                NAME,
                SURNAME,
                MIDDLE_NAME,
                DATE_OF_BIRTH,
                EMAIL,
                PHONE
        );
    }

    public static UserProfileResponse getUserProfileResponse() {
        return new UserProfileResponse(
                USER_GUID,
                NAME,
                SURNAME,
                MIDDLE_NAME,
                DATE_OF_BIRTH,
                EMAIL,
                PHONE,
                DEFAULT_PROFILE_STATUS,
                UPDATED,
                UPDATED
        );
    }

    public static UpdateUserProfileRequest getUpdateUserProfileRequest() {
        return new UpdateUserProfileRequest(
                JsonNullable.of(NAME),
                JsonNullable.of(SURNAME),
                JsonNullable.of(MIDDLE_NAME),
                JsonNullable.of(DATE_OF_BIRTH),
                JsonNullable.of(PHONE),
                JsonNullable.of(DEFAULT_PROFILE_STATUS)
        );
    }

    public static UpdateUserProfileRequest getUpdateUserProfileRequestWithOtherData() {
        return new UpdateUserProfileRequest(
                JsonNullable.of(OTHER_NAME),
                JsonNullable.of(OTHER_SURNAME),
                JsonNullable.of(OTHER_MIDDLE_NAME),
                JsonNullable.of(OTHER_DATE_OF_BIRTH),
                JsonNullable.of(OTHER_PHONE),
                JsonNullable.of(OTHER_PROFILE_STATUS)
        );
    }

    public static UserProfileResponse getUserProfileResponseWithOtherData() {
        return new UserProfileResponse(
                USER_GUID,
                OTHER_NAME,
               OTHER_SURNAME,
                OTHER_MIDDLE_NAME,
                OTHER_DATE_OF_BIRTH,
                EMAIL,
                OTHER_PHONE,
                OTHER_PROFILE_STATUS,
                UPDATED,
                UPDATED
        );
    }

    public static AllUserProfilesResponse getAllUserProfilesResponse() {
        return new AllUserProfilesResponse(List.of(getUserProfileResponse()));
    }
}
