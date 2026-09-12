package org.softspace.userprofile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.softspace.userprofile.dto.userprofile.request.CreateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.request.UpdateUserProfileRequest;
import org.softspace.userprofile.dto.userprofile.response.AllUserProfilesResponse;
import org.softspace.userprofile.dto.userprofile.response.UserProfileResponse;
import org.softspace.userprofile.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createNewUserProfile(
            @RequestBody @Valid CreateUserProfileRequest newUserProfile
    ) {
        UserProfileResponse createdNewUserProfileResponse = userProfileService.createUserProfile(newUserProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNewUserProfileResponse);
    }

    @GetMapping("/{guid}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable final UUID guid
    ) {
        UserProfileResponse userProfileResponse = userProfileService.getUserProfile(guid);
        return ResponseEntity.ok(userProfileResponse);
    }

    @GetMapping
    public ResponseEntity<AllUserProfilesResponse> getAllUserProfile() {
        AllUserProfilesResponse allUserProfileResponse = userProfileService.getAllUserProfile();
        return ResponseEntity.ok(allUserProfileResponse);
    }

    @PatchMapping("/{guid}")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable final UUID guid,
            @RequestBody @Valid UpdateUserProfileRequest updateUserProfileRequest
    ) {
        UserProfileResponse updatedUserProfileResponse = userProfileService.updateUserProfile(updateUserProfileRequest, guid);
        return ResponseEntity.ok(updatedUserProfileResponse);
    }

    @DeleteMapping("/{guid}")
    public ResponseEntity<Void> deleteUserProfile(
            @PathVariable final UUID guid
    ) {
        userProfileService.deleteUserProfile(guid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
