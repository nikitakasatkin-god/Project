package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;
    private final RequestRepository requestRepository;
    private final CarrierRepository carrierRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ExternalSystemStub externalSystemStub;

    public TripController(TripRepository tripRepository,
                          RequestRepository requestRepository,
                          CarrierRepository carrierRepository,
                          VehicleRepository vehicleRepository,
                          UserRepository userRepository,
                          ExternalSystemStub externalSystemStub) {
        this.tripRepository = tripRepository;
        this.requestRepository = requestRepository;
        this.carrierRepository = carrierRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.externalSystemStub = externalSystemStub;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping("/request/{requestId}")
    public List<Trip> getTripsByRequest(@PathVariable Long requestId) {
        return requestRepository.findById(requestId)
                .map(tripRepository::findByRequest)
                .orElse(List.of());
    }

    @PostMapping
    public ResponseEntity<?> createTrip(@RequestBody Map<String, Object> data) {
        Long requestId = Long.parseLong(data.get("requestId").toString());
        Request request = requestRepository.findById(requestId).orElse(null);

        if (request == null) return ResponseEntity.notFound().build();
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            return ResponseEntity.status(403).body("Заявка не в статусе 'В работе'");
        }

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Доступ запрещен");
        }

        // Расчет объема рейса (кратно 25)
        double totalVolume = request.getVolume();
        double assignedVolume = request.getTrips().stream().mapToDouble(Trip::getVolume).sum();
        double remainingVolume = totalVolume - assignedVolume;

        double tripVolume = remainingVolume >= 25 ? 25 : remainingVolume;

        Carrier carrier = carrierRepository.findById(Long.parseLong(data.get("carrierId").toString())).orElse(null);
        if (carrier == null) return ResponseEntity.badRequest().body("Перевозчик не найден");

        Vehicle vehicle = vehicleRepository.findById(Long.parseLong(data.get("vehicleId").toString())).orElse(null);
        if (vehicle == null) return ResponseEntity.badRequest().body("Транспорт не найден");

        Trip trip = new Trip();
        trip.setRequest(request);
        trip.setCarrier(carrier);
        trip.setVehiclePlate(vehicle.getPlateNumber());
        trip.setTrailerPlate(vehicle.getTrailerPlate());
        trip.setVehicleBrand(vehicle.getBrand() + " " + vehicle.getModel());
        trip.setDriverName(vehicle.getDriverName());
        trip.setTripDate(LocalDate.parse(data.get("tripDate").toString()));
        trip.setVolume(tripVolume);
        trip.setStatus(TripStatus.NEW);
        trip.setSequenceNumber(request.getTrips().size() + 1);

        Trip saved = tripRepository.save(trip);

        // Заглушка: отправка в систему диспетчеризации
        externalSystemStub.sendToDispatchSystem(saved);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTripStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Trip trip = tripRepository.findById(id).orElse(null);
        if (trip == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();
        String newStatusStr = data.get("status").toString();
        TripStatus newStatus = TripStatus.valueOf(newStatusStr);

        // Логист может обновить статусы: ARRIVED_LOADING, LOADED
        if (currentUser.getRole() == Role.LOGIST) {
            if (trip.getStatus() == TripStatus.NEW && newStatus == TripStatus.ARRIVED_LOADING) {
                trip.setStatus(TripStatus.ARRIVED_LOADING);
                tripRepository.save(trip);
                return ResponseEntity.ok(trip);
            }
            if (trip.getStatus() == TripStatus.ARRIVED_LOADING && newStatus == TripStatus.LOADED) {
                trip.setStatus(TripStatus.LOADED);
                tripRepository.save(trip);

                // Заглушка: отправка в систему диспетчеризации
                externalSystemStub.sendToDispatchSystem(trip);
                return ResponseEntity.ok(trip);
            }
        }

        // Диспетчер может обновить статусы (система диспетчеризации)
        if (currentUser.getRole() == Role.DISPATCHER || currentUser.getRole() == Role.ADMIN) {
            trip.setStatus(newStatus);
            tripRepository.save(trip);

            // Проверка: все ли рейсы обработаны
            Request request = trip.getRequest();
            boolean allProcessed = request.getTrips().stream()
                    .allMatch(t -> t.getStatus() == TripStatus.PROCESSED);

            if (allProcessed) {
                request.setStatus(RequestStatus.PROCESSED);
                requestRepository.save(request);
            }

            return ResponseEntity.ok(trip);
        }

        return ResponseEntity.status(403).body("Доступ запрещен");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(@PathVariable Long id) {
        Trip trip = tripRepository.findById(id).orElse(null);
        if (trip == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();

        // Только диспетчер может удалить рейс со статусом NEW
        if (trip.getStatus() == TripStatus.NEW &&
                (currentUser.getRole() == Role.DISPATCHER || currentUser.getRole() == Role.ADMIN)) {
            tripRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно");
    }
}