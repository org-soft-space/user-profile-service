package org.softspace.userprofile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.softspace.userprofile.DtoTestBuilder;
import org.softspace.userprofile.config.JacksonConfig;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.response.AllUserProfilesResponse;
import org.softspace.userprofile.dto.userprofile.response.UserProfileResponse;
import org.softspace.userprofile.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@ActiveProfiles("test")
@Import(JacksonConfig.class)
class UserProfileControllerTest {

    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private Tracer tracer;


    @Test
    void shouldCreateUserProfile() throws Exception {

        CreateUserProfileRequest newUserProfileRequest = DtoTestBuilder.getCreateUserProfileRequest();
        UserProfileResponse newUserProfileResponse = DtoTestBuilder.getUserProfileResponse();

        when(userProfileService.createUserProfile(newUserProfileRequest))
                .thenReturn(newUserProfileResponse);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserProfileRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(DtoTestBuilder.USER_GUID.toString()));

        verify(userProfileService).createUserProfile(any(CreateUserProfileRequest.class));
    }

    @Test
    void shouldGetUserProfile() throws Exception {

        UserProfileResponse newUserProfileResponse = DtoTestBuilder.getUserProfileResponse();

        when(userProfileService.getUserProfile(DtoTestBuilder.USER_GUID))
                .thenReturn(newUserProfileResponse);

        mockMvc.perform(get(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(DtoTestBuilder.USER_GUID.toString()));

        verify(userProfileService).getUserProfile(DtoTestBuilder.USER_GUID);
    }

    @Test
    void shouldGetAllUserProfile() throws Exception {

        UserProfileResponse newUserProfileResponse = DtoTestBuilder.getUserProfileResponse();
        AllUserProfilesResponse allUsers = new AllUserProfilesResponse(List.of(newUserProfileResponse));

        when(userProfileService.getAllUserProfile())
                .thenReturn(allUsers);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allUserProfiles").isArray())
                .andExpect(jsonPath("$.allUserProfiles.length()").value(1))
                .andExpect(jsonPath("$.allUserProfiles[0].guid").value(DtoTestBuilder.USER_GUID.toString()));

        verify(userProfileService).getAllUserProfile();
    }

    @Test
    void shouldUpdateUserProfile() throws Exception {

        when(userProfileService.updateUserProfile(
                any(UpdateUserProfileRequest.class),
                any(UUID.class)))
                .thenReturn(DtoTestBuilder.getUserProfileResponse());

        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DtoTestBuilder.getUpdateUserProfileRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(DtoTestBuilder.USER_GUID.toString()));

        verify(userProfileService).updateUserProfile(DtoTestBuilder.getUpdateUserProfileRequest(), DtoTestBuilder.USER_GUID);
    }

    @Test
    void shouldUpdateUserProfileWithNullInNullableDtoFields() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.of(null),
                JsonNullable.undefined(),
                JsonNullable.of(null),
                JsonNullable.undefined()
        );

        when(userProfileService.updateUserProfile(
                any(UpdateUserProfileRequest.class),
                any(UUID.class)))
                .thenReturn(DtoTestBuilder.getUserProfileResponse());

        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(DtoTestBuilder.USER_GUID.toString()));

        verify(userProfileService).updateUserProfile(request, DtoTestBuilder.USER_GUID);
    }

    @Test
    void shouldDeleteUserProfile() throws Exception {

        mockMvc.perform(delete(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userProfileService).deleteUserProfile(DtoTestBuilder.USER_GUID);
    }
}
