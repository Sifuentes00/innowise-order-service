package com.matvey.innowiseorderservice.repository;

import com.matvey.innowiseorderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id AND o.deleted = false")
    Optional<Order> findByIdWithItemsAndDeletedFalse(@Param("id") UUID id);

    Optional<Order> findByIdAndDeletedFalse(UUID id);

    List<Order> findByUserIdAndDeletedFalse(UUID userId);

    List<Order> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
