package com.meetruly.matching.repository;

import com.meetruly.matching.model.Like;
import com.meetruly.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByLikerAndLiked(User liker, User liked);

    @Query("SELECT COUNT(l) > 0 FROM Like l WHERE l.liker = :liker AND l.liked = :liked")
    boolean existsByLikerAndLiked(@Param("liker") User liker, @Param("liked") User liked);

    List<Like> findByLiker(User liker);

    List<Like> findByLiked(User liked);

    @Query("SELECT l FROM Like l WHERE l.liked = :user AND l.viewed = false")
    List<Like> findUnviewedLikesByUser(@Param("user") User user);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.liked = :user")
    long countLikesByUser(@Param("user") User user);

    @Query("SELECT l.liked FROM Like l WHERE l.liker = :user")
    List<User> findLikedUsersByUser(@Param("user") User user);

    @Query("SELECT l.liker FROM Like l WHERE l.liked = :user")
    List<User> findLikersByUser(@Param("user") User user);

    @Query("SELECT l FROM Like l WHERE l.liker = :liker AND l.liked = :liked AND EXISTS (SELECT l2 FROM Like l2 WHERE l2.liker = :liked AND l2.liked = :liker)")
    List<Like> findMutualLikes(@Param("liker") User liker, @Param("liked") User liked);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.createdAt >= :since")
    long countLikesSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT u.id, COUNT(l.id) AS like_count FROM users u JOIN likes l ON u.id = l.liked_id GROUP BY u.id ORDER BY like_count DESC", nativeQuery = true)
    Page<Object[]> findTopLikedUsers(Pageable pageable);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);
}