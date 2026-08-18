package com.careerforge.repository;

import com.careerforge.entity.Notification;
import com.careerforge.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUser(User user, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    Page<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
