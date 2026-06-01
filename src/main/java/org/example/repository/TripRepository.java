package org.example.repository;

import org.example.model.Request;
import org.example.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByRequest(Request request);
}