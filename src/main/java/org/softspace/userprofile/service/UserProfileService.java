package org.softspace.userprofile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.response.AllUserProfilesResponse;
import org.softspace.userprofile.dto.userprofile.response.UserProfileResponse;
import org.softspace.userprofile.entity.UserProfileEntity;
import org.softspace.userprofile.enums.ProfileStatus;
import org.softspace.userprofile.exception.EmailAlreadyExistsException;
import org.softspace.userprofile.exception.UserProfileNotFoundException;
import org.softspace.userprofile.repository.UserProfileRepository;
import org.softspace.userprofile.service.mapper.UserProfileMapper;
import org.softspace.userprofile.service.validator.UserProfileValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileStatus profileStatusDefault = ProfileStatus.ACTIVE;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileValidator userProfileValidator;

    @Transactional
    public UserProfileResponse createUserProfile(CreateUserProfileRequest newUserProfile) {
        Instant now = Instant.now();
        UUID newGuid = UUID.randomUUID();
        log.info("Create new user profile: {}", newGuid);

        String newEmail = newUserProfile.email();
        boolean emailExists = userProfileRepository.existsByEmailAndDeletedAtIsNull(newEmail);
        if (emailExists) {
            throw new EmailAlreadyExistsException(
                    "This email already exists.",
                    Map.of("email", newEmail)
            );
        }
        // Mapping from DTO to entity.
        UserProfileEntity userProfileEntity = userProfileMapper.createUserProfileRequestToUserProfileEntity(
                newUserProfile,
                newGuid,
                profileStatusDefault,
                now
        );
        // TODO: тест для DataIntegrityViolationException.  Метод save выкинул исключение.
        //  и проверить что пришел EmailAlreadyExistsException.
        // Create user profile
        try {
            UserProfileEntity createdUserProfile = userProfileRepository.save(userProfileEntity);
            return userProfileMapper.userProfileEntityToUserProfileResponse(createdUserProfile);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(
                    "This email already exists.",
                    ex,
                    Map.of("email", newEmail)
            );
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userGuid) {
        log.info("Find user profile: {}", userGuid);
        Optional<UserProfileEntity> userProfile = userProfileRepository.findByGuidAndDeletedAtIsNull(userGuid);
        UserProfileEntity userEntity = userProfile.orElseThrow(() -> {
            return new UserProfileNotFoundException(
                    "User profile not found",
                    Map.of("user guid", userGuid)
            );
        });
        return userProfileMapper.userProfileEntityToUserProfileResponse(userEntity);
    }

    @Transactional(readOnly = true)
    public AllUserProfilesResponse getAllUserProfile() {
        log.info("Find all user profiles.");
        List<UserProfileEntity> userProfileList = userProfileRepository.findAllByDeletedAtIsNull();
        return userProfileMapper.userProfileEntityListToAllUserProfilesResponse(userProfileList);
    }


    @Transactional
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest updateUserProfile, UUID userGuid) {
        log.info("Update user profile: {}", userGuid);
        userProfileValidator.validUpdateUserProfileRequest(updateUserProfile);

        Optional<UserProfileEntity> userProfileEntity = userProfileRepository.findByGuidAndDeletedAtIsNull(userGuid);
        UserProfileEntity user = userProfileEntity.orElseThrow(() -> {
            return new UserProfileNotFoundException(
                    "The user profile not found by guid: " + userGuid,
                    Map.of("User guid", userGuid)
            );
        });

        Instant updatedDateTime = Instant.now();
        applyChange(user, updateUserProfile, updatedDateTime);
        return userProfileMapper.userProfileEntityToUserProfileResponse(user);
    }

    @Transactional
    public void deleteUserProfile(UUID userGuid) {
        log.info("Delete user profile: {}", userGuid);
        Optional<UserProfileEntity> userProfile = userProfileRepository.findByGuidAndDeletedAtIsNull(userGuid);
        UserProfileEntity userProfileEntity = userProfile.orElseThrow(() -> {
            return new UserProfileNotFoundException(
                    "The user profile not found by guid: " + userGuid,
                    Map.of("user guid", userGuid)
            );
        });
        Instant now = Instant.now();
        userProfileEntity.setDeletedAt(now);
        userProfileEntity.setUpdatedAt(now);
        // TODO: Изучить lifecycle entity в Hibernate при наличие транзакций.
        //  и почему здесь можно убрать метод save?
//        userProfileRepository.save(userProfileEntity);
    }


    private void applyChange(UserProfileEntity userProfileEntity,
                             UpdateUserProfileRequest updateUserProfile,
                             Instant updatedTime
    ) {
        userProfileEntity.setUpdatedAt(updatedTime);

        JsonNullable<String> name = updateUserProfile.name();
        if (name.isPresent()) {
            userProfileEntity.setName(name.get());
        }
        JsonNullable<String> surname = updateUserProfile.surname();
        if (surname.isPresent()) {
            userProfileEntity.setSurname(surname.get());
        }
        JsonNullable<String> middleName = updateUserProfile.middleName();
        if (middleName.isPresent()) {
            userProfileEntity.setMiddleName(middleName.get());
        }
        JsonNullable<LocalDate> dateOfBirth = updateUserProfile.dateOfBirth();
        if (dateOfBirth.isPresent()) {
            userProfileEntity.setDateOfBirth(dateOfBirth.get());
        }
        JsonNullable<String> phone = updateUserProfile.phone();
        if (phone.isPresent()) {
            userProfileEntity.setPhone(phone.get());
        }
        JsonNullable<ProfileStatus> profileStatus = updateUserProfile.profileStatus();
        if (profileStatus.isPresent()) {
            userProfileEntity.setProfileStatus(profileStatus.get());
        }
    }
}
