package org.example.repository;

import org.example.model.TripStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TripStatusRepository extends JpaRepository<TripStatusEntity, Long> {
    Optional<TripStatusEntity> findByCode(String code);
    List<TripStatusEntity> findByActiveTrueOrderBySortOrderAsc();
    List<TripStatusEntity> findBySystemDefaultFalse();
}