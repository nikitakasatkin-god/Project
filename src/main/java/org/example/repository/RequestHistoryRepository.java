package org.example.repository;

import org.example.model.Request;
import org.example.model.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    List<RequestHistory> findByRequestOrderByChangedAtAsc(Request request);
    List<RequestHistory> findByRequestOrderByChangedAtDesc(Request request);
}