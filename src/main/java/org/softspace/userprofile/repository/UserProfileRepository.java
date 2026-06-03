package org.softspace.userprofile.repository;


import org.softspace.userprofile.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByGuidAndDeletedAtIsNull(UUID userProfileGuid);

    List<UserProfileEntity> findAllByDeletedAtIsNull();

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
