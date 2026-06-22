package org.example.repository;

import org.example.model.Trip;
import org.example.model.TripHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripHistoryRepository extends JpaRepository<TripHistory, Long> {
    List<TripHistory> findByTripOrderByChangedAtAsc(Trip trip);
}