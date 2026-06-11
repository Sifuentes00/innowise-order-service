package com.matvey.innowiseorderservice.repository;

import com.matvey.innowiseorderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndDeletedFalse(UUID id);

    List<Order> findByUserIdAndDeletedFalse(UUID userId);

    List<Order> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
