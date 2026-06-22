package org.example.repository;

import org.example.model.StatusMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StatusMappingRepository extends JpaRepository<StatusMapping, Long> {
    Optional<StatusMapping> findByDispatchStatusId(Long dispatchStatusId);
    Optional<StatusMapping> findByDispatchStatusName(String name);
    List<StatusMapping> findByActiveTrue();
}