package com.meetruly.user.repository;

import com.meetruly.core.constant.*;
import com.meetruly.user.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    List<UserProfile> findByCountry(Country country);

    List<UserProfile> findByCity(City city);

    Page<UserProfile> findByAgeGreaterThanEqualAndAgeLessThanEqual(Integer minAge, Integer maxAge, Pageable pageable);

    @Query("SELECT up FROM UserProfile up WHERE up.user.gender = :gender AND up.age BETWEEN :minAge AND :maxAge")
    Page<UserProfile> findByGenderAndAgeBetween(
            @Param("gender") Gender gender,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            Pageable pageable);

    @Query("SELECT up FROM UserProfile up JOIN up.interests i WHERE i = :interest")
    List<UserProfile> findByInterest(@Param("interest") Interest interest);

    @Query("SELECT up FROM UserProfile up WHERE up.relationshipType = :relationshipType")
    List<UserProfile> findByRelationshipType(@Param("relationshipType") RelationshipType relationshipType);

    @Query("SELECT up FROM UserProfile up WHERE up.relationshipStatus = :relationshipStatus")
    List<UserProfile> findByRelationshipStatus(@Param("relationshipStatus") RelationshipStatus relationshipStatus);

    @Query("SELECT up FROM UserProfile up WHERE up.user.id IN (SELECT DISTINCT up2.user.id FROM UserProfile up2 JOIN up2.interests i WHERE i IN :interests) ORDER BY SIZE(up.interests)")
    List<UserProfile> findByInterestsOrderByMatchCount(@Param("interests") List<Interest> interests, Pageable pageable);
}