package org.example.repository;

import org.example.model.DispatchStatusMapping;
import org.example.model.TripStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DispatchStatusMappingRepository extends JpaRepository<DispatchStatusMapping, Long> {
    Optional<DispatchStatusMapping> findByDispatchStatusId(Long dispatchStatusId);
    List<DispatchStatusMapping> findByActiveTrue();
    boolean existsByLocalStatus(TripStatusEntity localStatus);
}