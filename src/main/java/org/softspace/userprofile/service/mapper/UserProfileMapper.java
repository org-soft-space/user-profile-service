package org.softspace.userprofile.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.response.AllUserProfilesResponse;
import org.softspace.userprofile.dto.userprofile.response.UserProfileResponse;
import org.softspace.userprofile.entity.UserProfileEntity;
import org.softspace.userprofile.enums.ProfileStatus;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Primary
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "guid", source = "guid")
    @Mapping(target = "name", source = "createUserProfileRequest.name")
    @Mapping(target = "surname", source = "createUserProfileRequest.surname")
    @Mapping(target = "middleName", source = "createUserProfileRequest.middleName")
    @Mapping(target = "dateOfBirth", source = "createUserProfileRequest.dateOfBirth")
    @Mapping(target = "email", source = "createUserProfileRequest.email")
    @Mapping(target = "phone", source = "createUserProfileRequest.phone")
    @Mapping(target = "profileStatus", source = "profileStatus")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "deletedAt", ignore = true)
    UserProfileEntity createUserProfileRequestToUserProfileEntity(
            CreateUserProfileRequest createUserProfileRequest,
            UUID guid,
            ProfileStatus profileStatus,
            Instant now
    );

    UserProfileResponse userProfileEntityToUserProfileResponse(UserProfileEntity userProfileEntity);


    default AllUserProfilesResponse userProfileEntityListToAllUserProfilesResponse(List<UserProfileEntity> allUserProfiles) {
        List<UserProfileResponse> response = allUserProfiles.stream().map(this::userProfileEntityToUserProfileResponse).toList();
        return new AllUserProfilesResponse(response);
    }
}
