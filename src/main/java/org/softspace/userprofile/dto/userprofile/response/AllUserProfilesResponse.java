package org.softspace.userprofile.dto.userprofile.response;

import java.util.List;

public record AllUserProfilesResponse(
        List<UserProfileResponse> allUserProfiles
) {
}
