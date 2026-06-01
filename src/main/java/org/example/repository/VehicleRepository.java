package org.example.repository;

import org.example.model.Carrier;
import org.example.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCarrier(Carrier carrier);
}