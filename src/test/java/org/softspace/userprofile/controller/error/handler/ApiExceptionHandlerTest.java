package org.softspace.userprofile.controller.error.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.softspace.userprofile.DtoTestBuilder;
import org.softspace.userprofile.config.JacksonConfig;
import org.softspace.userprofile.controller.UserProfileController;
import org.softspace.userprofile.controller.error.ErrorCode;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.exception.EmailAlreadyExistsException;
import org.softspace.userprofile.exception.UserProfileNotFoundException;
import org.softspace.userprofile.exception.ValidationException;
import org.softspace.userprofile.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
class ApiExceptionHandlerTest {

    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private Tracer tracer;


    // EmailAlreadyExistsException
    @DisplayName("Create user profile. Email already exists. Negative.")
    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {

        CreateUserProfileRequest newUserProfileRequest = DtoTestBuilder.getCreateUserProfileRequest();


        when(userProfileService.createUserProfile(newUserProfileRequest))
                .thenThrow(new EmailAlreadyExistsException(
                        "Email already exists.",
                        Map.of("email", newUserProfileRequest.email())
                ));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserProfileRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.EMAIL_ALREADY_EXISTS.toString()))
                .andExpect(jsonPath("$.path").value(BASE_URL));

        verify(userProfileService).createUserProfile(newUserProfileRequest);
    }

    // UserProfileNotFoundException
    @DisplayName("Find user profile. User not found exception. Negative.")
    @Test
    void shouldReturnNotFoundExceptionWhenGettingUnknownProfile() throws Exception {

        when(userProfileService.getUserProfile(DtoTestBuilder.USER_GUID))
                .thenThrow(UserProfileNotFoundException.class);

        mockMvc.perform(get(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.USER_PROFILE_NOT_FOUND.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService).getUserProfile(DtoTestBuilder.USER_GUID);
    }

    // MethodArgumentNotValidException
    @DisplayName("Create user profile. Method argument not valid exception. Negative.")
    @ParameterizedTest(name = "name={0}, surname={1}, email={2}, phone={3}")
    @MethodSource("createUserProfileVariants")
    void shouldCreateUserProfileValueException(
            String name,
            String surname,
            String email,
            String phone
    ) throws Exception {

        CreateUserProfileRequest newUserProfileRequest = new CreateUserProfileRequest(
                name,
                surname,
                DtoTestBuilder.MIDDLE_NAME,
                DtoTestBuilder.DATE_OF_BIRTH,
                email,
                phone
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserProfileRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_ERROR.toString()))
                .andExpect(jsonPath("$.path").value(BASE_URL));

        verify(userProfileService, never()).createUserProfile(any(CreateUserProfileRequest.class));
    }

    private static Stream<Arguments> createUserProfileVariants() {
        return Stream.of(
                Arguments.of(null, DtoTestBuilder.SURNAME, DtoTestBuilder.EMAIL, DtoTestBuilder.PHONE),
                Arguments.of(DtoTestBuilder.NAME, null, DtoTestBuilder.EMAIL, DtoTestBuilder.PHONE),
                Arguments.of(DtoTestBuilder.NAME, DtoTestBuilder.SURNAME, null, DtoTestBuilder.PHONE),
                Arguments.of(DtoTestBuilder.NAME, DtoTestBuilder.SURNAME, DtoTestBuilder.EMAIL, "NotAPhonNumber")
        );
    }

    // RuntimeException
    @DisplayName("Delete user profile. Internal exception. Negative.")
    @Test
    void shouldReturnInternalExceptionHandle() throws Exception {
        doThrow(RuntimeException.class)
                .when(userProfileService).deleteUserProfile(DtoTestBuilder.USER_GUID);

        mockMvc.perform(delete(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.INTERNAL_ERROR.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService).deleteUserProfile(DtoTestBuilder.USER_GUID);
    }

    // UnrecognizedPropertyException
    @DisplayName("Update user profile. Unrecognized property exception. Negative.")
    @Test
    void shouldReturnUnrecognizedPropertyException() throws Exception {
        // Given
        String json = """
                {
                  "name": "user name",
                  "unrecognized": "name"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.FIELD_NOT_ALLOWED.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService, never()).updateUserProfile(any(), eq(DtoTestBuilder.USER_GUID));
    }

    // InvalidFormatException
    @DisplayName("Update user profile. Invalid format exception. Negative.")
    @Test
    void shouldReturnInvalidFormatException() throws Exception {
        // Given
        String json = """
                {
                  "name": "user name",
                  "profileStatus": "ACTIVE 123"
                }
                """;

        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.FIELD_INVALID.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService, never()).updateUserProfile(any(), eq(DtoTestBuilder.USER_GUID));
    }

    // MismatchedInputException
    @DisplayName("Update user profile. Mismatched input exception. Negative.")
    @Test
    void shouldReturnMismatchedInputException() throws Exception {
        // Given
        String json = """
                  name": "user name",
                  "profileStatus": "ACTIVE"
                }
                """;
        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.BAD_REQUEST.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService, never()).updateUserProfile(any(), eq(DtoTestBuilder.USER_GUID));
    }

    // ValidationException
    @DisplayName("Update user profile. Validation exception. Negative.")
    @Test
    void shouldReturnValidationException() throws Exception {
        // Given
        UpdateUserProfileRequest request = DtoTestBuilder.getUpdateUserProfileRequest();

        // When
        when(userProfileService.updateUserProfile(request, DtoTestBuilder.USER_GUID)).thenThrow(new ValidationException(
                "No valid data.",
                Map.of("guid", DtoTestBuilder.USER_GUID)
        ));

        // Then
        mockMvc.perform(patch(BASE_URL + "/{guid}", DtoTestBuilder.USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect((jsonPath("$.status").value(ErrorCode.VALIDATION_ERROR.toString())))
                .andExpect((jsonPath("$.path").value(BASE_URL + "/" + DtoTestBuilder.USER_GUID)));

        verify(userProfileService).updateUserProfile(any(), eq(DtoTestBuilder.USER_GUID));
    }

    // EmailAlreadyExistsException with TraceId
    @DisplayName("Create user profile. Email already exists exception with TraceId. Negative.")
    @Test
    void shouldReturnConflictWhenEmailAlreadyExistsWithTraceId() throws Exception {

        // Given
        String traceId = "abc";

        Span span = Mockito.mock(Span.class);
        TraceContext traceContext = Mockito.mock(TraceContext.class);
        CreateUserProfileRequest newUserProfileRequest = DtoTestBuilder.getCreateUserProfileRequest();

        // When
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);

        when(userProfileService.createUserProfile(newUserProfileRequest))
                .thenThrow(new EmailAlreadyExistsException(
                        "Email already exists.",
                        Map.of("email", newUserProfileRequest.email())
                ));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserProfileRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.EMAIL_ALREADY_EXISTS.toString()))
                .andExpect(jsonPath("$.path").value(BASE_URL))
                .andExpect(jsonPath("$.traceId").value(traceId));

        verify(userProfileService).createUserProfile(newUserProfileRequest);
    }
}
