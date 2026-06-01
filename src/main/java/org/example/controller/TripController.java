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
                .map(request -> tripRepository.findByRequest(request).stream()
                        .filter(t -> t.getStatus() != TripStatus.DELETED)
                        .toList())
                .orElse(List.of());
    }

    @PostMapping
    public ResponseEntity<?> createTrip(@RequestBody Map<String, Object> data) {
        Long requestId = Long.parseLong(data.get("requestId").toString());
        Request request = requestRepository.findById(requestId).orElse(null);

        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            return ResponseEntity.status(403).body("Заявка не в статусе 'В работе'");
        }

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Доступ запрещен. Только диспетчер может добавлять рейсы.");
        }

        // Проверка - не распределен ли уже весь объем (учитываем только активные рейсы)
        double totalVolume = request.getVolume();
        double assignedVolume = request.getTrips().stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .mapToDouble(Trip::getVolume).sum();
        double remainingVolume = totalVolume - assignedVolume;

        if (remainingVolume <= 0) {
            return ResponseEntity.badRequest().body("Объем заявки полностью распределен");
        }

        // Получаем перевозчика
        Carrier carrier = carrierRepository.findById(Long.parseLong(data.get("carrierId").toString())).orElse(null);
        if (carrier == null) {
            return ResponseEntity.badRequest().body("Перевозчик не найден");
        }

        // Получаем выбранный автомобиль (если выбран)
        Vehicle vehicle = null;
        String vehiclePlate = "";
        String vehicleBrand = "";

        if (data.get("vehicleId") != null && !data.get("vehicleId").toString().isEmpty()) {
            Long vehicleId = Long.parseLong(data.get("vehicleId").toString());
            vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle != null) {
                vehiclePlate = vehicle.getPlateNumber();
                vehicleBrand = vehicle.getBrand() + " " + vehicle.getModel();
            }
        }

        // Получаем данные из формы
        String trailerPlate = data.get("trailerPlate") != null ? data.get("trailerPlate").toString() : "";
        String driverName = data.get("driverName") != null ? data.get("driverName").toString() : "";

        // Если водитель не указан, берем из автомобиля
        String finalDriverName = driverName;
        if (finalDriverName.isEmpty() && vehicle != null && vehicle.getDriverName() != null) {
            finalDriverName = vehicle.getDriverName();
        }

        // Проверка уникальности автомобиля для этой заявки (учитываем только активные)
        final String finalVehiclePlate = vehiclePlate;
        if (vehicle != null && !finalVehiclePlate.isEmpty()) {
            boolean vehicleAlreadyUsed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .anyMatch(t -> finalVehiclePlate.equals(t.getVehiclePlate()));
            if (vehicleAlreadyUsed) {
                return ResponseEntity.badRequest().body("Этот автомобиль уже назначен на другой рейс по данной заявке");
            }
        }

        // Проверка уникальности прицепа для этой заявки
        final String finalTrailerPlate = trailerPlate;
        if (!finalTrailerPlate.isEmpty()) {
            boolean trailerAlreadyUsed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .anyMatch(t -> finalTrailerPlate.equals(t.getTrailerPlate()));
            if (trailerAlreadyUsed) {
                return ResponseEntity.badRequest().body("Этот прицеп уже назначен на другой рейс по данной заявке");
            }
        }

        // Проверка уникальности водителя для этой заявки
        final String finalDriverNameForCheck = finalDriverName;
        if (!finalDriverNameForCheck.isEmpty()) {
            boolean driverAlreadyUsed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .anyMatch(t -> finalDriverNameForCheck.equals(t.getDriverName()));
            if (driverAlreadyUsed) {
                return ResponseEntity.badRequest().body("Этот водитель уже назначен на другой рейс по данной заявке");
            }
        }

        // Расчет объема рейса
        double tripVolume = remainingVolume >= 25 ? 25 : remainingVolume;

        Trip trip = new Trip();
        trip.setRequest(request);
        trip.setCarrier(carrier);
        trip.setVehiclePlate(vehiclePlate);
        trip.setTrailerPlate(trailerPlate);
        trip.setVehicleBrand(vehicleBrand);
        trip.setDriverName(finalDriverName);
        trip.setTripDate(LocalDate.parse(data.get("tripDate").toString()));
        trip.setVolume(tripVolume);
        trip.setStatus(TripStatus.NEW);
        trip.setSequenceNumber(request.getTrips().size() + 1);

        Trip saved = tripRepository.save(trip);

        externalSystemStub.sendToDispatchSystem(saved);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTripStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Trip trip = tripRepository.findById(id).orElse(null);
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();
        String newStatusStr = data.get("status").toString();

        TripStatus newStatus;
        try {
            newStatus = TripStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Неизвестный статус: " + newStatusStr);
        }

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
                externalSystemStub.sendToDispatchSystem(trip);
                return ResponseEntity.ok(trip);
            }
            return ResponseEntity.status(403).body("Невозможно обновить статус");
        }

        // Диспетчер или админ могут обновить статусы
        if (currentUser.getRole() == Role.DISPATCHER || currentUser.getRole() == Role.ADMIN) {
            // Для отмены рейса
            if (newStatus == TripStatus.CANCELLED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.CANCELLED);
                tripRepository.save(trip);
                return ResponseEntity.ok(trip);
            }

            // Для удаления - просто меняем статус, не удаляем запись
            if (newStatus == TripStatus.DELETED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.DELETED);
                tripRepository.save(trip);
                return ResponseEntity.ok(trip);
            }

            trip.setStatus(newStatus);
            tripRepository.save(trip);

            // Проверка: все ли активные рейсы обработаны
            Request request = trip.getRequest();
            boolean allProcessed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
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
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (trip.getStatus() == TripStatus.NEW &&
                (currentUser.getRole() == Role.DISPATCHER || currentUser.getRole() == Role.ADMIN)) {
            // Вместо удаления - меняем статус на DELETED
            trip.setStatus(TripStatus.DELETED);
            tripRepository.save(trip);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно. Рейс не в статусе 'Новый' или недостаточно прав.");
    }
}