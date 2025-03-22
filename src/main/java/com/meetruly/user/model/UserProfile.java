package com.meetruly.user.model;

import com.meetruly.core.constant.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String firstName;

    private String lastName;

    @Column
    private Integer age;

    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor;

    @Enumerated(EnumType.STRING)
    private HairColor hairColor;

    @Column
    private Integer height;

    @Column
    private Integer weight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Enumerated(EnumType.STRING)
    private Set<Interest> interests = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String partnerPreferences;

    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    private RelationshipStatus relationshipStatus;

    @Enumerated(EnumType.STRING)
    @Column
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column
    private City city;

    private String profileImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_gallery", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "image_url")
    private Set<String> gallery = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}