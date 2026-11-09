package com.muntinlupa.barangay_ease.repository;

import com.muntinlupa.barangay_ease.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserIdAndUserRole(Integer userId, String userRole);
    List<Notification> findByUserIdAndUserRoleOrderByCreatedAtDesc(Integer userId, String userRole);
}
