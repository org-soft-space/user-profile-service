package org.softspace.userprofile.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.DtoTestBuilder;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @InjectMocks
    private UserProfileService userProfileService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private UserProfileValidator userProfileValidator;

    private static UserProfileEntity userProfileEntity;

    private static UUID USER_GUID = DtoTestBuilder.USER_GUID;

    @BeforeEach
    void setUp() {
        userProfileEntity = new UserProfileEntity();
        userProfileEntity.setGuid(USER_GUID);
        userProfileEntity.setEmail(DtoTestBuilder.EMAIL);
        userProfileEntity.setName(DtoTestBuilder.NAME);
        userProfileEntity.setSurname(DtoTestBuilder.SURNAME);
        userProfileEntity.setPhone(DtoTestBuilder.PHONE);
    }

    @Test
    void createNewUserProfileSuccessfullyTest() {

        // Set up
        ProfileStatus defaultStatus = ProfileStatus.ACTIVE;
        CreateUserProfileRequest createUserProfileRequest = DtoTestBuilder.getCreateUserProfileRequest();
        UserProfileResponse userProfileResponse = DtoTestBuilder.getUserProfileResponse();

        // When
        when(userProfileMapper.createUserProfileRequestToUserProfileEntity(
                eq(createUserProfileRequest),
                any(UUID.class),
                any(ProfileStatus.class),
                any(Instant.class)))
                .thenReturn(userProfileEntity);
        when(userProfileRepository.save(userProfileEntity))
                .thenReturn(userProfileEntity);
        when(userProfileMapper.userProfileEntityToUserProfileResponse(userProfileEntity))
                .thenReturn(userProfileResponse);

        // Execute
        UserProfileResponse result = userProfileService.createUserProfile(createUserProfileRequest);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.guid(), USER_GUID);
        Assertions.assertEquals(result.profileStatus(), DtoTestBuilder.DEFAULT_PROFILE_STATUS);

        verify(userProfileMapper).createUserProfileRequestToUserProfileEntity(
                eq(createUserProfileRequest),
                any(UUID.class),
                any(ProfileStatus.class),
                any(Instant.class)
        );
        verify(userProfileRepository).save(userProfileEntity);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    @Test
    void emailAlreadyExistsExceptionTest() {
        // Set up
        String email = DtoTestBuilder.EMAIL;
        CreateUserProfileRequest newUserProfile = DtoTestBuilder.getCreateUserProfileRequest();

        // When
        when(userProfileRepository.existsByEmailAndDeletedAtIsNull(email))
                .thenReturn(true);

        // Execute
        EmailAlreadyExistsException exception = Assertions.assertThrows(
                EmailAlreadyExistsException.class,
                () -> userProfileService.createUserProfile(newUserProfile)
        );

        // Then
        Assertions.assertNotNull(exception);
        verify(userProfileRepository).existsByEmailAndDeletedAtIsNull(email);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void dataIntegrityViolationExceptionTest() {
        // Set up
        String email = DtoTestBuilder.EMAIL;
        CreateUserProfileRequest newUserProfile = DtoTestBuilder.getCreateUserProfileRequest();

        // When
        when(userProfileRepository.existsByEmailAndDeletedAtIsNull(email))
                .thenReturn(false);
        when(userProfileRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        // Execute
        EmailAlreadyExistsException exception = Assertions.assertThrows(
                EmailAlreadyExistsException.class,
                () -> userProfileService.createUserProfile(newUserProfile)
        );

        // Then
        Assertions.assertNotNull(exception);
        verify(userProfileRepository).existsByEmailAndDeletedAtIsNull(email);
        verify(userProfileRepository).save(any());
    }

    // успешный PATCH
    @Test
    void updateUserProfileSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = DtoTestBuilder.getUpdateUserProfileRequestWithOtherData();
        UserProfileResponse userProfileResponse = DtoTestBuilder.getUserProfileResponseWithOtherData();

        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        when(userProfileMapper.userProfileEntityToUserProfileResponse(userProfileEntity))
                .thenReturn(userProfileResponse);

        // Execute
        UserProfileResponse updatedUserProfileResponseResult = userProfileService.updateUserProfile(
                updateUserProfile, USER_GUID
        );

        // Then
        Assertions.assertSame(updatedUserProfileResponseResult, userProfileResponse);

        Assertions.assertEquals(updateUserProfile.name().get(), userProfileEntity.getName());
        Assertions.assertEquals(updateUserProfile.surname().get(), userProfileEntity.getSurname());
        Assertions.assertEquals(updateUserProfile.middleName().get(), userProfileEntity.getMiddleName());
        Assertions.assertEquals(updateUserProfile.dateOfBirth().get(), userProfileEntity.getDateOfBirth());
        Assertions.assertEquals(updateUserProfile.phone().get(), userProfileEntity.getPhone());
        Assertions.assertEquals(updateUserProfile.profileStatus().get(), userProfileEntity.getProfileStatus());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updateNameSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.of(DtoTestBuilder.OTHER_NAME),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.name().get(), userProfileEntity.getName());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updateSurnameSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.of(DtoTestBuilder.OTHER_SURNAME),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.surname().get(), userProfileEntity.getSurname());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updateMiddleNameSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(DtoTestBuilder.OTHER_MIDDLE_NAME),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.middleName().get(), userProfileEntity.getMiddleName());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updateDateOfBirthSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(DtoTestBuilder.OTHER_DATE_OF_BIRTH),
                JsonNullable.undefined(),
                JsonNullable.undefined()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.dateOfBirth().get(), userProfileEntity.getDateOfBirth());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updatePhoneSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(DtoTestBuilder.OTHER_PHONE),
                JsonNullable.undefined()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.phone().get(), userProfileEntity.getPhone());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // успешный PATCH
    @Test
    void updateProfileStatusSuccessfullyTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(DtoTestBuilder.OTHER_PROFILE_STATUS)
                );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        // Execute
        userProfileService.updateUserProfile(updateUserProfile, USER_GUID);
        // Then
        Assertions.assertEquals(updateUserProfile.profileStatus().get(), userProfileEntity.getProfileStatus());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    // PATCH с null для nullable-поля
    @Test
    void nullableUpdateUserProfileSuccessfullyTest() {
        userProfileEntity.setMiddleName(DtoTestBuilder.MIDDLE_NAME);
        userProfileEntity.setPhone(DtoTestBuilder.PHONE);

        UpdateUserProfileRequest updateUserProfile = new UpdateUserProfileRequest(
                JsonNullable.of("TestName"),
                JsonNullable.of("TestSurname"),
                JsonNullable.of(null),
                JsonNullable.of(LocalDate.of(1990, 10, 1)),
                JsonNullable.of(null),
                JsonNullable.of(ProfileStatus.INACTIVE)
        );


        UserProfileResponse userProfileResponse = new UserProfileResponse(
                USER_GUID,
                "TestName",
                "TestSurname",
                null,
                LocalDate.of(1990, 10, 1),
                "user@test.com",
                null,
                ProfileStatus.INACTIVE,
                Instant.now(),
                Instant.now()
        );
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID)).thenReturn(Optional.of(userProfileEntity));
        when(userProfileMapper.userProfileEntityToUserProfileResponse(userProfileEntity))
                .thenReturn(userProfileResponse);

        // Execute
        UserProfileResponse updatedUserProfileResponseResult = userProfileService.updateUserProfile(updateUserProfile, USER_GUID);

        // Then
        Assertions.assertNotNull(updatedUserProfileResponseResult);
        Assertions.assertNull(updatedUserProfileResponseResult.middleName());
        Assertions.assertNull(updatedUserProfileResponseResult.phone());
        Assertions.assertEquals(updatedUserProfileResponseResult.guid(), USER_GUID);

        Assertions.assertNull(userProfileEntity.getMiddleName());
        Assertions.assertNull(userProfileEntity.getPhone());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileMapper).userProfileEntityToUserProfileResponse(userProfileEntity);
    }

    @Test
    void updateUserProfileNotFoundExceptionTest() {
        // Given
        UpdateUserProfileRequest updateUserProfile = DtoTestBuilder.getUpdateUserProfileRequest();

        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.empty());

        // Then
        Assertions.assertThrows(
                UserProfileNotFoundException.class,
                () -> userProfileService.updateUserProfile(updateUserProfile, USER_GUID)
        );

        verify(userProfileRepository, never()).save(any());
        verify(userProfileMapper, never()).userProfileEntityToUserProfileResponse(any());
    }

    @Test
    void getUserProfileSuccessfullyTest() {

        // Given
        UserProfileResponse userProfileResponse = DtoTestBuilder.getUserProfileResponse();

        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));
        when(userProfileMapper.userProfileEntityToUserProfileResponse(userProfileEntity))
                .thenReturn(userProfileResponse);

        UserProfileResponse result = userProfileService.getUserProfile(USER_GUID);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.guid(), userProfileResponse.guid());
        Assertions.assertEquals(result.name(), userProfileResponse.name());
        Assertions.assertEquals(result.surname(), userProfileResponse.surname());

        verify(userProfileMapper).userProfileEntityToUserProfileResponse(any());
    }

    @Test
    void getUserProfileNotFoundExceptionTest() {

        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.empty());

        // Then
        Assertions.assertThrows(
                UserProfileNotFoundException.class,
                () -> userProfileService.getUserProfile(USER_GUID)
        );

        verify(userProfileMapper, never()).userProfileEntityToUserProfileResponse(any());
    }

    @Test
    void getAllUserProfileSuccessfullyTest() {

        // Given
        List<UserProfileEntity> userList = List.of(userProfileEntity);
        AllUserProfilesResponse response = DtoTestBuilder.getAllUserProfilesResponse();

        // When
        when(userProfileRepository.findAllByDeletedAtIsNull())
                .thenReturn(userList);
        when(userProfileMapper.userProfileEntityListToAllUserProfilesResponse(userList))
                .thenReturn(response);

        // Execute
        AllUserProfilesResponse result = userProfileService.getAllUserProfile();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.allUserProfiles());
        Assertions.assertFalse(result.allUserProfiles().isEmpty());

        verify(userProfileRepository).findAllByDeletedAtIsNull();
        verify(userProfileMapper).userProfileEntityListToAllUserProfilesResponse(userList);
    }

    @Test
    void deleteUserProfileSuccessfullyTest() {

        // Given
        userProfileEntity.setDeletedAt(null);

        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.of(userProfileEntity));

        // Then
        Assertions.assertDoesNotThrow(
                () -> userProfileService.deleteUserProfile(USER_GUID)
        );

        Assertions.assertNotNull(userProfileEntity.getDeletedAt());
        Assertions.assertNotNull(userProfileEntity.getUpdatedAt());
        Assertions.assertEquals(
                userProfileEntity.getDeletedAt(),
                userProfileEntity.getUpdatedAt()
        );

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
    }

    @Test
    void deleteUserProfileNotFoundExceptionTest() {
        // When
        when(userProfileRepository.findByGuidAndDeletedAtIsNull(USER_GUID))
                .thenReturn(Optional.empty());

        // Then
        Assertions.assertThrows(
                UserProfileNotFoundException.class,
                () -> userProfileService.deleteUserProfile(USER_GUID)
        );
        Assertions.assertNull(userProfileEntity.getDeletedAt());

        verify(userProfileRepository).findByGuidAndDeletedAtIsNull(USER_GUID);
        verify(userProfileRepository, never()).save(any());
    }
}
