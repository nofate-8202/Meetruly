package com.meetruly.user.repository;

import com.meetruly.core.constant.*;
import com.meetruly.user.model.User;
import com.meetruly.user.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private User maleUser;
    private User femaleUser;
    private User otherUser;
    private UserProfile maleProfile;
    private UserProfile femaleProfile;
    private UserProfile otherProfile;

    @BeforeEach
    void setUp() {
        
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        
        maleUser = new User();
        maleUser.setUsername("maleuser");
        maleUser.setEmail("male@example.com");
        maleUser.setPassword("password");
        maleUser.setGender(Gender.MALE);
        maleUser.setRole(UserRole.USER);
        maleUser.setCreatedAt(now);
        maleUser.setUpdatedAt(now);

        femaleUser = new User();
        femaleUser.setUsername("femaleuser");
        femaleUser.setEmail("female@example.com");
        femaleUser.setPassword("password");
        femaleUser.setGender(Gender.FEMALE);
        femaleUser.setRole(UserRole.USER);
        femaleUser.setCreatedAt(now);
        femaleUser.setUpdatedAt(now);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setGender(Gender.OTHER);
        otherUser.setRole(UserRole.USER);
        otherUser.setCreatedAt(now);
        otherUser.setUpdatedAt(now);

        
        maleUser = userRepository.save(maleUser);
        femaleUser = userRepository.save(femaleUser);
        otherUser = userRepository.save(otherUser);

        
        maleUser.setApproved(true);
        femaleUser.setApproved(true);
        otherUser.setApproved(true);
        maleUser.setProfileCompleted(true);
        femaleUser.setProfileCompleted(true);
        otherUser.setProfileCompleted(true);

        maleUser = userRepository.save(maleUser);
        femaleUser = userRepository.save(femaleUser);
        otherUser = userRepository.save(otherUser);

        
        maleProfile = new UserProfile();
        maleProfile.setUser(maleUser);
        maleProfile.setFirstName("John");
        maleProfile.setLastName("Doe");
        maleProfile.setAge(30);
        maleProfile.setHeight(180);
        maleProfile.setWeight(80);
        maleProfile.setEyeColor(EyeColor.BLUE);
        maleProfile.setHairColor(HairColor.BROWN);
        maleProfile.setCountry(Country.BULGARIA);
        maleProfile.setCity(City.SOFIA);
        maleProfile.setRelationshipType(RelationshipType.DATING);
        maleProfile.setRelationshipStatus(RelationshipStatus.SINGLE);
        maleProfile.setPartnerPreferences("Looking for a woman between 25-35 years old");
        maleProfile.setProfileImageUrl("male-profile.jpg");

        
        Set<Interest> maleInterests = new HashSet<>();
        maleInterests.add(Interest.SPORT);
        maleInterests.add(Interest.TECHNOLOGY);
        maleInterests.add(Interest.MOVIES);
        maleProfile.setInterests(maleInterests);

        
        Set<String> maleGallery = new HashSet<>();
        maleGallery.add("male-gallery-1.jpg");
        maleGallery.add("male-gallery-2.jpg");
        maleProfile.setGallery(maleGallery);

        femaleProfile = new UserProfile();
        femaleProfile.setUser(femaleUser);
        femaleProfile.setFirstName("Jane");
        femaleProfile.setLastName("Smith");
        femaleProfile.setAge(28);
        femaleProfile.setHeight(165);
        femaleProfile.setWeight(60);
        femaleProfile.setEyeColor(EyeColor.GREEN);
        femaleProfile.setHairColor(HairColor.BLONDE);
        femaleProfile.setCountry(Country.BULGARIA);
        femaleProfile.setCity(City.PLOVDIV);
        femaleProfile.setRelationshipType(RelationshipType.LONG_TERM);
        femaleProfile.setRelationshipStatus(RelationshipStatus.DIVORCED);
        femaleProfile.setPartnerPreferences("Looking for a man between 28-40 years old");
        femaleProfile.setProfileImageUrl("female-profile.jpg");

        
        Set<Interest> femaleInterests = new HashSet<>();
        femaleInterests.add(Interest.MOVIES);
        femaleInterests.add(Interest.BOOKS);
        femaleInterests.add(Interest.TRAVEL);
        femaleProfile.setInterests(femaleInterests);

        
        Set<String> femaleGallery = new HashSet<>();
        femaleGallery.add("female-gallery-1.jpg");
        femaleGallery.add("female-gallery-2.jpg");
        femaleProfile.setGallery(femaleGallery);

        otherProfile = new UserProfile();
        otherProfile.setUser(otherUser);
        otherProfile.setFirstName("Alex");
        otherProfile.setLastName("Taylor");
        otherProfile.setAge(35);
        otherProfile.setHeight(175);
        otherProfile.setWeight(70);
        otherProfile.setEyeColor(EyeColor.BROWN);
        otherProfile.setHairColor(HairColor.BLACK);
        otherProfile.setCountry(Country.GERMANY);
        otherProfile.setCity(City.BERLIN);
        otherProfile.setRelationshipType(RelationshipType.FRIENDSHIP);
        otherProfile.setRelationshipStatus(RelationshipStatus.SINGLE);
        otherProfile.setPartnerPreferences("Looking for friendship with people of any age");
        otherProfile.setProfileImageUrl("other-profile.jpg");

        
        Set<Interest> otherInterests = new HashSet<>();
        otherInterests.add(Interest.ART);
        otherInterests.add(Interest.MUSIC);
        otherInterests.add(Interest.PHOTOGRAPHY);
        otherProfile.setInterests(otherInterests);

        
        Set<String> otherGallery = new HashSet<>();
        otherGallery.add("other-gallery-1.jpg");
        otherGallery.add("other-gallery-2.jpg");
        otherProfile.setGallery(otherGallery);

        
        maleProfile = userProfileRepository.save(maleProfile);
        femaleProfile = userProfileRepository.save(femaleProfile);
        otherProfile = userProfileRepository.save(otherProfile);

        
        assertEquals(3, userProfileRepository.count(), "Should have 3 user profiles saved in the test database");
    }

    @Test
    void testBasicCrud() {
        
        UserProfile profile = new UserProfile();
        profile.setUser(maleUser); 
        profile.setFirstName("Test");
        profile.setLastName("Profile");
        profile.setAge(25);

        
        
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("password");
        newUser.setGender(Gender.MALE);
        newUser.setRole(UserRole.USER);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        newUser = userRepository.save(newUser);

        profile.setUser(newUser);

        UserProfile savedProfile = userProfileRepository.save(profile);
        assertNotNull(savedProfile.getId());

        
        Optional<UserProfile> foundProfile = userProfileRepository.findById(savedProfile.getId());
        assertTrue(foundProfile.isPresent());
        assertEquals("Test", foundProfile.get().getFirstName());

        
        foundProfile.get().setFirstName("Updated");
        userProfileRepository.save(foundProfile.get());

        UserProfile updatedProfile = userProfileRepository.findById(savedProfile.getId()).get();
        assertEquals("Updated", updatedProfile.getFirstName());

        
        userProfileRepository.delete(updatedProfile);
        assertFalse(userProfileRepository.findById(savedProfile.getId()).isPresent());
    }

    @Test
    void testFindByUserId() {
        Optional<UserProfile> foundProfile = userProfileRepository.findByUserId(maleUser.getId());
        assertTrue(foundProfile.isPresent());
        assertEquals("John", foundProfile.get().getFirstName());
        assertEquals(maleUser.getId(), foundProfile.get().getUser().getId());
    }

    @Test
    void testFindByCountry() {
        List<UserProfile> bulgarianProfiles = userProfileRepository.findByCountry(Country.BULGARIA);
        assertEquals(2, bulgarianProfiles.size());

        List<UserProfile> germanProfiles = userProfileRepository.findByCountry(Country.GERMANY);
        assertEquals(1, germanProfiles.size());
        assertEquals("Alex", germanProfiles.get(0).getFirstName());
    }

    @Test
    void testFindByCity() {
        List<UserProfile> sofiaProfiles = userProfileRepository.findByCity(City.SOFIA);
        assertEquals(1, sofiaProfiles.size());
        assertEquals("John", sofiaProfiles.get(0).getFirstName());

        List<UserProfile> plovdivProfiles = userProfileRepository.findByCity(City.PLOVDIV);
        assertEquals(1, plovdivProfiles.size());
        assertEquals("Jane", plovdivProfiles.get(0).getFirstName());
    }

    @Test
    void testFindByAgeGreaterThanEqualAndAgeLessThanEqual() {
        Pageable pageable = PageRequest.of(0, 10);

        
        Page<UserProfile> youngProfiles = userProfileRepository.findByAgeGreaterThanEqualAndAgeLessThanEqual(25, 30, pageable);
        assertEquals(2, youngProfiles.getTotalElements());

        
        Page<UserProfile> olderProfiles = userProfileRepository.findByAgeGreaterThanEqualAndAgeLessThanEqual(31, 40, pageable);
        assertEquals(1, olderProfiles.getTotalElements());
        assertEquals("Alex", olderProfiles.getContent().get(0).getFirstName());
    }

    @Test
    void testFindByGenderAndAgeBetween() {
        Pageable pageable = PageRequest.of(0, 10);

        
        Page<UserProfile> youngMales = userProfileRepository.findByGenderAndAgeBetween(Gender.MALE, 25, 35, pageable);
        assertEquals(1, youngMales.getTotalElements());
        assertEquals("John", youngMales.getContent().get(0).getFirstName());

        
        Page<UserProfile> youngFemales = userProfileRepository.findByGenderAndAgeBetween(Gender.FEMALE, 25, 30, pageable);
        assertEquals(1, youngFemales.getTotalElements());
        assertEquals("Jane", youngFemales.getContent().get(0).getFirstName());
    }

    @Test
    void testFindByInterest() {
        
        List<UserProfile> movieLovers = userProfileRepository.findByInterest(Interest.MOVIES);
        assertEquals(2, movieLovers.size());

        
        Set<String> expectedNames = new HashSet<>(Arrays.asList("John", "Jane"));
        Set<String> actualNames = new HashSet<>();
        for (UserProfile profile : movieLovers) {
            actualNames.add(profile.getFirstName());
        }
        assertEquals(expectedNames, actualNames);

        
        List<UserProfile> musicLovers = userProfileRepository.findByInterest(Interest.MUSIC);
        assertEquals(1, musicLovers.size());
        assertEquals("Alex", musicLovers.get(0).getFirstName());
    }

    @Test
    void testFindByRelationshipType() {
        
        List<UserProfile> datingProfiles = userProfileRepository.findByRelationshipType(RelationshipType.DATING);
        assertEquals(1, datingProfiles.size());
        assertEquals("John", datingProfiles.get(0).getFirstName());

        
        List<UserProfile> longTermProfiles = userProfileRepository.findByRelationshipType(RelationshipType.LONG_TERM);
        assertEquals(1, longTermProfiles.size());
        assertEquals("Jane", longTermProfiles.get(0).getFirstName());
    }

    @Test
    void testFindByRelationshipStatus() {
        
        List<UserProfile> singleProfiles = userProfileRepository.findByRelationshipStatus(RelationshipStatus.SINGLE);
        assertEquals(2, singleProfiles.size());

        
        List<UserProfile> divorcedProfiles = userProfileRepository.findByRelationshipStatus(RelationshipStatus.DIVORCED);
        assertEquals(1, divorcedProfiles.size());
        assertEquals("Jane", divorcedProfiles.get(0).getFirstName());
    }

    @Test
    void testFindByInterestsOrderByMatchCount() {
        Pageable pageable = PageRequest.of(0, 10);

        
        List<Interest> searchInterests = Arrays.asList(Interest.MOVIES);

        
        List<UserProfile> matchingProfiles = userProfileRepository.findByInterestsOrderByMatchCount(searchInterests, pageable);

        
        assertEquals(2, matchingProfiles.size());

        
        searchInterests = new ArrayList<>(femaleProfile.getInterests());

        
        assertTrue(searchInterests.contains(Interest.MOVIES));
        assertTrue(searchInterests.contains(Interest.BOOKS));
        assertTrue(searchInterests.contains(Interest.TRAVEL));

        
        matchingProfiles = userProfileRepository.findByInterestsOrderByMatchCount(searchInterests, pageable);

        
        assertTrue(matchingProfiles.size() >= 2);

        
        
        
        boolean foundFemale = false;
        boolean foundMale = false;

        for (UserProfile profile : matchingProfiles) {
            if ("Jane".equals(profile.getFirstName())) {
                foundFemale = true;
            } else if ("John".equals(profile.getFirstName())) {
                foundMale = true;
            }
        }

        assertTrue(foundFemale, "Female profile should be in the results");
        assertTrue(foundMale, "Male profile should be in the results");
    }
}