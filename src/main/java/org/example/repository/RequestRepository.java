package org.example.repository;

import org.example.model.Division;
import org.example.model.Request;
import org.example.model.RequestStatus;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByOwner(User owner);
    List<Request> findByDivision(Division division);
    List<Request> findByStatus(RequestStatus status);
}