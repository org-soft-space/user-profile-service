package org.softspace.userprofile.repository;


import org.softspace.userprofile.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

}
