package org.softspace.userprofile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.softspace.userprofile.controller.error.ErrorCode;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.enums.ProfileStatus;
import org.softspace.userprofile.result.ObjectBuilder;
import org.softspace.userprofile.result.object.UserProfileDbResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@AutoConfigureMockMvc
public class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE_URL = "/api/v1/users";
    private static final UUID REAL_USER_GUID = UUID.fromString("c479a77f-0386-4a0f-9245-0927d25b0e77");
    private static final UUID GUID = UUID.fromString("11111111-1111-1111-1111-111111111111");


    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    // -------Positive tests-----------------------------------------------------------------------------------
    // CREATE

    /**
     * Create a new user profile.
     *
     * Проверка запроса на создание нового пользователя.
     * Проверяются все поля которые пришёл в ответе с полями запроса.
     * После, jdbcTemplate делает два запроса в БД:
     *   1) находит объект по email, проверяет на его наличие и его поля на соответствие запросу.
     *   2) проверяет, что объект в БД находится в единственном экземпляре.
     *
     */

    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Create new user profile. Positive.")
    @Test
    void createNewUserProfile() throws Exception {

        CreateUserProfileRequest newUserProfileRequest = new CreateUserProfileRequest(
                "NAME",
                "SURNAME",
                "MIDDLE_NAME",
                LocalDate.of(1995, 5, 20),
                "new-user@softspace.org",
                "+79991234567"
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserProfileRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").isNotEmpty())
                .andExpect(jsonPath("$.name").value(newUserProfileRequest.name()))
                .andExpect(jsonPath("$.surname").value(newUserProfileRequest.surname()))
                .andExpect(jsonPath("$.middleName").value(newUserProfileRequest.middleName()))
                .andExpect(jsonPath("$.dateOfBirth").value(newUserProfileRequest.dateOfBirth().toString()))
                .andExpect(jsonPath("$.email").value(newUserProfileRequest.email()))
                .andExpect(jsonPath("$.phone").value(newUserProfileRequest.phone()))
                .andExpect(jsonPath("$.profileStatus").value(ProfileStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        UserProfileDbResult userResult = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE email = ? AND deleted_at IS NULL",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                newUserProfileRequest.email()
        );

        Integer countUsersWithEmail = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_profile WHERE email = ? AND deleted_at IS NULL",
                Integer.class,
                newUserProfileRequest.email()
        );

        Assertions.assertThat(userResult).isNotNull();
        Assertions.assertThat(userResult.guid()).isNotNull();
        Assertions.assertThat(userResult.name()).isEqualTo(newUserProfileRequest.name());
        Assertions.assertThat(userResult.surname()).isEqualTo(newUserProfileRequest.surname());
        Assertions.assertThat(userResult.middleName()).isEqualTo(newUserProfileRequest.middleName());
        Assertions.assertThat(userResult.dateOfBirth()).isEqualTo(newUserProfileRequest.dateOfBirth());
        Assertions.assertThat(userResult.email()).isEqualTo(newUserProfileRequest.email());
        Assertions.assertThat(userResult.phone()).isEqualTo(newUserProfileRequest.phone());
        Assertions.assertThat(userResult.profileStatus()).isEqualTo(ProfileStatus.ACTIVE);
        Assertions.assertThat(userResult.updatedAt()).isNotNull();
        Assertions.assertThat(userResult.createdAt()).isNotNull();

        Assertions.assertThat(countUsersWithEmail).isEqualTo(1);
    }

    // GET
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Find user profile. Positive.")
    @Test
    void shouldGetUserProfile() throws Exception {

        mockMvc.perform(get(BASE_URL + "/{guid}", REAL_USER_GUID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(REAL_USER_GUID.toString()));

    }

    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Find all user profile. Positive.")
    @Test
    void shouldGetAllUserProfile() throws Exception {

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allUserProfiles").isArray())
                .andExpect(jsonPath("$.allUserProfiles.length()").isNotEmpty());
    }

    // UPDATE
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )

    @DisplayName("Update user profile. Positive.")
    @Test
    void shouldUpdateUserProfile() throws Exception {
        String updatedName = "updated NAME";
        String updatedSurname = "updated SURNAME";
        String updatedMiddleName = "updated MIDDLE_NAME";
        LocalDate updatedDateOfBirth = LocalDate.of(1986, 10, 11);
        String updatedPhone = "+79987654321";
        ProfileStatus updatedProfileStatus = ProfileStatus.BLOCKED;

        String json = """
                {
                  "name": "%s",
                  "surname": "%s",
                  "middleName": "%s",
                  "dateOfBirth": "%s",
                  "phone": "%s",
                  "profileStatus": "%s"
                }
                """.formatted(
                updatedName,
                updatedSurname,
                updatedMiddleName,
                updatedDateOfBirth,
                updatedPhone,
                updatedProfileStatus
        );


        UserProfileDbResult userBeforeUpdate = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE guid = ? AND deleted_at IS NULL",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                REAL_USER_GUID
        );


        mockMvc.perform(patch(BASE_URL + "/{guid}", REAL_USER_GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.guid").value(REAL_USER_GUID.toString()))
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.surname").value(updatedSurname))
                .andExpect(jsonPath("$.middleName").value(updatedMiddleName))
                .andExpect(jsonPath("$.dateOfBirth").value(updatedDateOfBirth.toString()))
                .andExpect(jsonPath("$.phone").value(updatedPhone))
                .andExpect(jsonPath("$.profileStatus").value(updatedProfileStatus.toString()))
                .andExpect(jsonPath("$.createdAt").value(userBeforeUpdate.createdAt().toString()))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        UserProfileDbResult userAfterUpdate = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE guid = ? AND deleted_at IS NULL",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                REAL_USER_GUID
        );

        Assertions.assertThat(userAfterUpdate.updatedAt()).isNotEqualTo(userBeforeUpdate.updatedAt());
        Assertions.assertThat(userAfterUpdate.createdAt()).isEqualTo(userBeforeUpdate.createdAt());

        Assertions.assertThat(userAfterUpdate.name()).isEqualTo(updatedName);
        Assertions.assertThat(userAfterUpdate.surname()).isEqualTo(updatedSurname);
        Assertions.assertThat(userAfterUpdate.middleName()).isEqualTo(updatedMiddleName);
        Assertions.assertThat(userAfterUpdate.dateOfBirth()).isEqualTo(updatedDateOfBirth);
        Assertions.assertThat(userAfterUpdate.phone()).isEqualTo(updatedPhone);
        Assertions.assertThat(userAfterUpdate.email()).isEqualTo(userBeforeUpdate.email());
        Assertions.assertThat(userAfterUpdate.profileStatus()).isEqualTo(updatedProfileStatus);
    }

    // DELETE
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Delete user profile. Positive.")
    @Test
    void shouldDeleteUserProfile() throws Exception {
        UserProfileDbResult userProfileDbResultBeforeDelete = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE guid = ?",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                REAL_USER_GUID
        );

        mockMvc.perform(delete(BASE_URL + "/{guid}", REAL_USER_GUID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(delete(BASE_URL + "/{guid}", REAL_USER_GUID))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USER_PROFILE_NOT_FOUND"));

        UserProfileDbResult userProfileDbResultAfterDelete = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE guid = ?",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                REAL_USER_GUID
        );

        Assertions.assertThat(userProfileDbResultAfterDelete).isNotNull();
        Assertions.assertThat(userProfileDbResultAfterDelete.createdAt()).isEqualTo(userProfileDbResultBeforeDelete.createdAt());
        Assertions.assertThat(userProfileDbResultAfterDelete.deletedAt()).isNotNull();
        Assertions.assertThat(userProfileDbResultAfterDelete.updatedAt()).isAfter(userProfileDbResultBeforeDelete.updatedAt());
    }

    // -------Negative tests-----------------------------------------------------------------------------------

    // CREATE ------------------------------------------

    /**
     * Should conflict with email.
     *
     * Отправляет запрос на создание нового пользователя с полем email,
     * которое уже существует в БД.
     * Ожидает получение 409 ошибки.
     * Проверяет поля ошибки: status, path, details.email.
     * После, jdbcTemplate делает запрос в БД с проверкой,
     * что объект в БД находится в единственном экземпляре.
     */
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Create user profile. Email conflict. Negative.")
    @Test
    void shouldConflictWithEmail() throws Exception {

        CreateUserProfileRequest newUserRequest = new CreateUserProfileRequest(
                "NAME",
                "SURNAME",
                "MIDDLE_NAME",
                LocalDate.of(1995, 5, 20),
                "user@softspace.org",
                "+79991234567"
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL))
                .andExpect(jsonPath("$.details.email").isNotEmpty());

        Integer countUsersWithEmail = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_profile WHERE email = ? AND deleted_at IS NULL",
                Integer.class,
                newUserRequest.email()
        );

        Assertions.assertThat(countUsersWithEmail).isEqualTo(1);
    }

    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/truncate.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Create user profile. Not valid field. Negative.")
    @ParameterizedTest(name = "fieldName={0}, name{1}, surname={2}, middleName={3}, dateOfBirth={4}, email={5}, phone={6}")
    @MethodSource("notValidFieldParameters")
    void shouldNotValidFieldNameInCreateNewUser(
            String fieldName,
            String name,
            String surname,
            String middleName,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) throws Exception {
        String countUsersSqlQuery = "SELECT COUNT(*) FROM USER_PROFILE";

        CreateUserProfileRequest newUserRequest = new CreateUserProfileRequest(
                name,
                surname,
                middleName,
                dateOfBirth,
                email,
                phone
        );

        Integer numUsersBefore = jdbcTemplate.queryForObject(countUsersSqlQuery, Integer.class);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL))
                .andExpect(jsonPath("$.details." + fieldName).isNotEmpty());

        Integer numUsersAfter = jdbcTemplate.queryForObject(countUsersSqlQuery, Integer.class);

        Assertions.assertThat(numUsersBefore).isEqualTo(numUsersAfter);
    }

    private static Stream<Arguments> notValidFieldParameters() {
        return Stream.of(
                Arguments.of(
                        "name",
                        " ",
                        "SURNAME",
                        "MIDDLE_NAME",
                        LocalDate.of(1995, 5, 20),
                        "user@softspace.org",
                        "+79991234567"
                ),
                Arguments.of(
                        "surname",
                        "NAME",
                        " ",
                        "MIDDLE_NAME",
                        LocalDate.of(1995, 5, 20),
                        "user@softspace.org",
                        "+79991234567"
                ),
                Arguments.of(
                        "email",
                        "NAME",
                        "SURNAME",
                        "MIDDLE_NAME",
                        LocalDate.of(1995, 5, 20),
                        " ",
                        "+79991234567"
                ),
                Arguments.of(
                        "email",
                        "NAME",
                        "SURNAME",
                        "MIDDLE_NAME",
                        LocalDate.of(1995, 5, 20),
                        "123",
                        "+79991234567"
                ),
                Arguments.of(
                        "phone",
                        "NAME",
                        "SURNAME",
                        "MIDDLE_NAME",
                        LocalDate.of(1995, 5, 20),
                        "user@softspace.org",
                        "123"
                )
        );
    }

    @DisplayName("Create user profile. Not valid data type of DataOfBirth. Negative.")
    @Test
    void shouldNotValidDataTypeOfDataOfBirthFieldInCreateNewUser() throws Exception {

        String newUserRequest = """
                {
                    "name": "%s",
                    "surname": "%s",
                    "middleName": "%s",
                    "dateOfBirth": "%s",
                    "email": "%s",
                    "phone": "%s"
                }
                """.formatted(
                "NAME",
                "SURNAME",
                "MIDDLE_NAME",
                "123",
                "user@softspace.org",
                "+79987654321"
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.FIELD_INVALID.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL))
                .andExpect(jsonPath("$.details").isNotEmpty())
                .andExpect(jsonPath("$.details.fieldName").value("dateOfBirth"));

    }

    // GET ------------------------------------------
    @DisplayName("Find user profile. Not found exception. Negative.")
    @Test
    void shouldUserNotFoundExceptionInFindUser() throws Exception {


        mockMvc.perform(get(BASE_URL + "/{guid}", GUID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_PROFILE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    // UPDATE ------------------------------------------
    @DisplayName("Update user profile. Not found exception. Negative.")
    @Test
    void shouldUserNotFoundExceptionInUpdateUser() throws Exception {

        String json = """
                {
                  "name": "%s"
                }
                """.formatted(
                "NAME"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_PROFILE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @DisplayName("Update user profile. Unrecognized property exception. Negative.")
    @Test
    void shouldUnrecognizedPropertyExceptionInUpdateUser() throws Exception {

        String newUserRequest = """
                {
                    "name": "%s",
                    "UNDEFINED": "%s"
                }
                """.formatted(
                "NAME",
                "SURNAME"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.FIELD_NOT_ALLOWED.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details").isNotEmpty())
                .andExpect(jsonPath("$.details.fieldName").value("UNDEFINED"));
    }


    @DisplayName("Update user profile. Incorrect json format. Negative.")
    @Test
    void shouldIncorrectJsonFormatInUpdateUser() throws Exception {

        String badJsonFormat = """
                {
                  name: "%s"
                }
                """.formatted(
                "NAME"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJsonFormat))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID));
    }

    @DisplayName("Update user profile. Invalid field. Negative.")
    @Test
    void shouldInvalidFieldInUpdateUser() throws Exception {

        String json = """
                {
                  "name": "%s",
                  "profileStatus": "%s"
                }
                """.formatted(
                "NAME",
                "UNKNOWN"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.FIELD_INVALID.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details.fieldName").value("profileStatus"));

    }

    @DisplayName("Update user profile. Not valid data type of dataOfBirth field. Negative.")
    @Test
    void shouldNotValidDataTypeOfDataOfBirthFieldInUpdateUser() throws Exception {

        String newUserRequest = """
                {
                    "name": "%s",
                    "surname": "%s",
                    "middleName": "%s",
                    "dateOfBirth": "%s",
                    "phone": "%s"
                }
                """.formatted(
                "NAME",
                "SURNAME",
                "MIDDLE_NAME",
                "123",
                "+79987654321"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.FIELD_INVALID.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details").isNotEmpty())
                .andExpect(jsonPath("$.details.fieldName").value("dateOfBirth"));

    }

    @DisplayName("Update user profile. Phone field validation error. Negative.")
    @Test
    void shouldPhoneFieldValidationErrorInUpdateUser() throws Exception {

        String json = """
                {
                  "name": "%s",
                  "phone": "%s"
                }
                """.formatted(
                "NAME",
                "123"
        );

        mockMvc.perform(patch(BASE_URL + "/{guid}", GUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details.phone").isNotEmpty());
    }

    // DELETE ------------------------------------------
    @DisplayName("Delete user profile. User not found exception. Negative.")
    @Test
    void shouldUserNotFoundExceptionInDeleteUser() throws Exception {

        mockMvc.perform(delete(BASE_URL + "/{guid}", GUID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_PROFILE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + GUID))
                .andExpect(jsonPath("$.details").isNotEmpty());

    }

    @Sql(
            scripts = "classpath:sql/user-profile/insert.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
            scripts = "classpath:sql/user-profile/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    @DisplayName("Delete user profile. Not allow double delete. Negative.")
    @Test
    void shouldNotAllowDoubleDeleteUser() throws Exception {

        // First deletion.
        mockMvc.perform(delete(BASE_URL + "/{guid}", REAL_USER_GUID))
                .andExpect(status().isNoContent());

        // Check for object deletion.
        UserProfileDbResult deletedUser = jdbcTemplate.queryForObject(
                "SELECT * FROM user_profile WHERE guid = ?",
                (rs, rowNum) -> ObjectBuilder.getUserProfileDbResult(rs),
                REAL_USER_GUID
        );
        Assertions.assertThat(deletedUser.deletedAt()).isNotNull();

        // Re-deletion.
        mockMvc.perform(delete(BASE_URL + "/{guid}", REAL_USER_GUID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_PROFILE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + REAL_USER_GUID))
                .andExpect(jsonPath("$.details").isNotEmpty());

    }
}
