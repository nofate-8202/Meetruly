package com.meetruly.matching.repository;

import com.meetruly.matching.model.ProfileView;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, UUID> {

    List<ProfileView> findByViewer(User viewer);

    List<ProfileView> findByViewed(User viewed);

    Page<ProfileView> findByViewed(User viewed, Pageable pageable);

    @Query("SELECT pv FROM ProfileView pv WHERE pv.viewed = :user AND pv.notificationSent = false")
    List<ProfileView> findUnnotifiedViewsByUser(@Param("user") User user);

    @Query("SELECT COUNT(pv) FROM ProfileView pv WHERE pv.viewed = :user")
    long countViewsByUser(@Param("user") User user);

    @Query("SELECT COUNT(pv) FROM ProfileView pv WHERE pv.viewedAt >= :since")
    long countProfileViewsSince(@Param("since") LocalDateTime since);

    @Query("SELECT pv.viewer, COUNT(pv) FROM ProfileView pv WHERE pv.viewed = :user GROUP BY pv.viewer ORDER BY COUNT(pv) DESC")
    List<Object[]> findTopViewersByUser(@Param("user") User user, Pageable pageable);
}