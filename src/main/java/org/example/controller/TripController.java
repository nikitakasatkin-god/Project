package org.example.controller;

import org.example.dto.TripDto;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.DispatchSyncService;
import org.example.service.ExternalSystemStub;
import org.example.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private static final Logger log = LoggerFactory.getLogger(TripController.class);

    private final TripRepository tripRepository;
    private final TripHistoryRepository tripHistoryRepository;
    private final RequestRepository requestRepository;
    private final CarrierRepository carrierRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ExternalSystemStub externalSystemStub;
    private final SyncService syncService;
    private final DispatchSyncService dispatchSyncService;

    public TripController(TripRepository tripRepository,
                          TripHistoryRepository tripHistoryRepository,
                          RequestRepository requestRepository,
                          CarrierRepository carrierRepository,
                          VehicleRepository vehicleRepository,
                          UserRepository userRepository,
                          ExternalSystemStub externalSystemStub,
                          SyncService syncService,
                          DispatchSyncService dispatchSyncService) {
        this.tripRepository = tripRepository;
        this.tripHistoryRepository = tripHistoryRepository;
        this.requestRepository = requestRepository;
        this.carrierRepository = carrierRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.externalSystemStub = externalSystemStub;
        this.syncService = syncService;
        this.dispatchSyncService = dispatchSyncService;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private String getStatusDisplayNameByCode(String statusCode) {
        if (statusCode == null) return null;
        switch (statusCode) {
            case "NEW": return "Новый";
            case "ARRIVED_LOADING": return "Прибыл на погрузку";
            case "LOADED": return "Погружен";
            case "IN_TRANSIT": return "В пути";
            case "ARRIVED_UNLOADING": return "Прибыл на выгрузку";
            case "UNLOADED": return "Выгружен";
            case "PROCESSED": return "Обработан";
            case "CANCELLED": return "Отменен";
            case "DELETED": return "Удален";
            default: return statusCode;
        }
    }

    private void addHistory(Trip trip, String status, String changedBy, String userName) {
        TripHistory history = new TripHistory();
        history.setTrip(trip);
        history.setStatus(status);
        history.setChangedBy(changedBy);
        history.setUserName(userName);
        history.setFromDispatch(false);
        history.setStatusCode(status);
        history.setStatusDisplay(getStatusDisplayNameByCode(status));
        tripHistoryRepository.save(history);
    }

    @GetMapping
    public List<TripDto> getTrips(@RequestParam(required = false) String type) {
        User currentUser = getCurrentUser();
        List<Trip> trips;

        if (currentUser.getRole() == Role.ADMIN) {
            trips = tripRepository.findAll();
        } else if (currentUser.getRole() == Role.DISPATCHER) {
            trips = tripRepository.findByRequest_Division(currentUser.getDivision());
        } else {
            return List.of();
        }

        if (type != null && !type.isEmpty()) {
            trips = trips.stream()
                    .filter(t -> t.getRequest().getProductType().name().equals(type))
                    .collect(Collectors.toList());
        }

        return trips.stream().map(trip -> {
            TripDto dto = new TripDto();
            dto.setId(trip.getId());
            dto.setRequestId(trip.getRequest() != null ? trip.getRequest().getId() : null);
            dto.setCarrierName(trip.getCarrier() != null ? trip.getCarrier().getName() : null);
            dto.setVehiclePlate(trip.getVehiclePlate());
            dto.setTrailerPlate(trip.getTrailerPlate());
            dto.setVehicleBrand(trip.getVehicleBrand());
            dto.setDriverName(trip.getDriverName());
            dto.setTripDate(trip.getTripDate());
            dto.setVolume(trip.getVolume());
            dto.setStatus(trip.getStatus());
            dto.setSyncedToDispatch(trip.getSyncedToDispatch());
            dto.setSequenceNumber(trip.getSequenceNumber());
            dto.setDispatchStatusName(trip.getDispatchStatusName());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(trip -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", trip.getId());
                    response.put("requestId", trip.getRequest() != null ? trip.getRequest().getId() : null);
                    response.put("request", trip.getRequest());
                    response.put("carrier", trip.getCarrier());
                    response.put("carrierName", trip.getCarrier() != null ? trip.getCarrier().getName() : null);
                    response.put("vehiclePlate", trip.getVehiclePlate());
                    response.put("trailerPlate", trip.getTrailerPlate());
                    response.put("vehicleBrand", trip.getVehicleBrand());
                    response.put("driverName", trip.getDriverName());
                    response.put("tripDate", trip.getTripDate());
                    response.put("volume", trip.getVolume());
                    response.put("status", trip.getStatus());
                    response.put("syncedToDispatch", trip.getSyncedToDispatch());
                    response.put("syncedAt", trip.getSyncedAt());
                    response.put("sequenceNumber", trip.getSequenceNumber());
                    response.put("createdAt", trip.getCreatedAt());
                    response.put("dispatchStatusId", trip.getDispatchStatusId());
                    response.put("dispatchStatusName", trip.getDispatchStatusName());

                    List<Map<String, Object>> historyList = new ArrayList<>();
                    for (TripHistory history : trip.getHistory()) {
                        Map<String, Object> h = new HashMap<>();
                        h.put("id", history.getId());
                        h.put("changedAt", history.getChangedAt());
                        h.put("status", history.getStatus());
                        h.put("statusCode", history.getStatusCode());
                        h.put("statusDisplay", history.getStatusDisplay());
                        h.put("changedBy", history.getChangedBy());
                        h.put("userName", history.getUserName());
                        h.put("dispatchStatusName", history.getDispatchStatusName());
                        h.put("fromDispatch", history.getFromDispatch());
                        historyList.add(h);
                    }
                    response.put("history", historyList);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getTripHistory(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(trip -> {
                    List<Map<String, Object>> historyList = new ArrayList<>();
                    for (TripHistory history : tripHistoryRepository.findByTripOrderByChangedAtAsc(trip)) {
                        Map<String, Object> h = new HashMap<>();
                        h.put("id", history.getId());
                        h.put("changedAt", history.getChangedAt());
                        h.put("status", history.getStatus());
                        h.put("statusCode", history.getStatusCode());
                        h.put("statusDisplay", history.getStatusDisplay());
                        h.put("changedBy", history.getChangedBy());
                        h.put("userName", history.getUserName());
                        h.put("dispatchStatusName", history.getDispatchStatusName());
                        h.put("fromDispatch", history.getFromDispatch());
                        historyList.add(h);
                    }
                    return ResponseEntity.ok(historyList);
                })
                .orElse(ResponseEntity.notFound().build());
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

        double totalVolume = request.getVolume();
        double assignedVolume = request.getTrips().stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .mapToDouble(Trip::getVolume).sum();
        double remainingVolume = totalVolume - assignedVolume;

        if (remainingVolume <= 0) {
            return ResponseEntity.badRequest().body("Объем заявки полностью распределен");
        }

        Carrier carrier = carrierRepository.findById(Long.parseLong(data.get("carrierId").toString())).orElse(null);
        if (carrier == null) {
            return ResponseEntity.badRequest().body("Перевозчик не найден");
        }

        if (!data.containsKey("vehicleId") || data.get("vehicleId").toString().isEmpty()) {
            return ResponseEntity.badRequest().body("Выберите госномер автомобиля");
        }

        if (!data.containsKey("driverName") || data.get("driverName").toString().isEmpty()) {
            return ResponseEntity.badRequest().body("Выберите водителя");
        }

        if (!data.containsKey("tripDate") || data.get("tripDate").toString().isEmpty()) {
            return ResponseEntity.badRequest().body("Выберите дату рейса");
        }

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

        String trailerPlate = data.get("trailerPlate") != null ? data.get("trailerPlate").toString() : "";
        String driverName = data.get("driverName") != null ? data.get("driverName").toString() : "";

        final String finalVehiclePlate = vehiclePlate;
        if (!finalVehiclePlate.isEmpty()) {
            boolean vehicleAlreadyUsed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .anyMatch(t -> finalVehiclePlate.equals(t.getVehiclePlate()));
            if (vehicleAlreadyUsed) {
                return ResponseEntity.badRequest().body("Этот автомобиль уже назначен на другой рейс по данной заявке");
            }
        }

        final String finalDriverName = driverName;
        if (!finalDriverName.isEmpty()) {
            boolean driverAlreadyUsed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .anyMatch(t -> finalDriverName.equals(t.getDriverName()));
            if (driverAlreadyUsed) {
                return ResponseEntity.badRequest().body("Этот водитель уже назначен на другой рейс по данной заявке");
            }
        }

        double tripVolume = remainingVolume >= 25 ? 25 : remainingVolume;

        Trip trip = new Trip();
        trip.setRequest(request);
        trip.setCarrier(carrier);
        trip.setVehiclePlate(vehiclePlate);
        trip.setTrailerPlate(trailerPlate);
        trip.setVehicleBrand(vehicleBrand);
        trip.setDriverName(driverName);
        trip.setTripDate(LocalDate.parse(data.get("tripDate").toString()));
        trip.setVolume(tripVolume);
        trip.setStatus(TripStatus.NEW);
        trip.setSyncedToDispatch(false);
        trip.setSequenceNumber(request.getTrips().size() + 1);

        Trip saved = tripRepository.save(trip);

        addHistory(saved, "NEW", "user:" + currentUser.getUsername(), currentUser.getFullName());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTrip(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Trip trip = tripRepository.findById(id).orElse(null);
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.DISPATCHER) {
            return ResponseEntity.status(403).body("Доступ запрещен");
        }

        if (trip.getStatus() != TripStatus.NEW) {
            return ResponseEntity.status(403).body("Редактирование возможно только для рейсов в статусе 'Новый'");
        }

        if (data.containsKey("carrierId")) {
            Carrier carrier = carrierRepository.findById(Long.parseLong(data.get("carrierId").toString())).orElse(null);
            if (carrier != null) {
                trip.setCarrier(carrier);
            }
        }

        if (data.containsKey("vehicleId")) {
            String vehicleIdStr = data.get("vehicleId").toString();
            if (vehicleIdStr != null && !vehicleIdStr.isEmpty()) {
                Long vehicleId = Long.parseLong(vehicleIdStr);
                Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
                if (vehicle != null) {
                    trip.setVehiclePlate(vehicle.getPlateNumber());
                    trip.setVehicleBrand(vehicle.getBrand() + " " + vehicle.getModel());
                    if (!data.containsKey("driverName") || data.get("driverName").toString().isEmpty()) {
                        trip.setDriverName(vehicle.getDriverName());
                    }
                }
            }
        }

        if (data.containsKey("vehiclePlate")) {
            trip.setVehiclePlate(data.get("vehiclePlate").toString());
        }

        if (data.containsKey("trailerPlate")) {
            trip.setTrailerPlate(data.get("trailerPlate").toString());
        }

        if (data.containsKey("driverName")) {
            trip.setDriverName(data.get("driverName").toString());
        }

        if (data.containsKey("tripDate")) {
            trip.setTripDate(LocalDate.parse(data.get("tripDate").toString()));
        }

        Trip saved = tripRepository.save(trip);

        addHistory(saved, "EDITED", "user:" + currentUser.getUsername(), currentUser.getFullName());

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

        log.info("=== ОБНОВЛЕНИЕ СТАТУСА РЕЙСА {} ===", id);
        log.info("Текущий статус: {}", trip.getStatus());
        log.info("Новый статус: {}", newStatus);
        log.info("Пользователь: {} ({})", currentUser.getUsername(), currentUser.getRole());

        if (currentUser.getRole() == Role.LOGIST) {
            if (trip.getStatus() == TripStatus.NEW && newStatus == TripStatus.ARRIVED_LOADING) {
                trip.setStatus(TripStatus.ARRIVED_LOADING);
                tripRepository.save(trip);
                addHistory(trip, "ARRIVED_LOADING", "user:" + currentUser.getUsername(), currentUser.getFullName());
                log.info("✅ Рейс {} изменен на ARRIVED_LOADING", trip.getId());
                return ResponseEntity.ok(trip);
            }

            if (trip.getStatus() == TripStatus.ARRIVED_LOADING && newStatus == TripStatus.LOADED) {
                log.info("🚛 ЛОГИСТ переводит рейс {} в LOADED", trip.getId());

                trip.setStatus(TripStatus.LOADED);
                tripRepository.save(trip);
                addHistory(trip, "LOADED", "user:" + currentUser.getUsername(), currentUser.getFullName());

                externalSystemStub.sendToDispatchSystem(trip);

                log.info("✅ Рейс {} переведен в LOADED", trip.getId());
                return ResponseEntity.ok(trip);
            }

            return ResponseEntity.status(403).body("Невозможно обновить статус");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            if (newStatus == TripStatus.CANCELLED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.CANCELLED);
                tripRepository.save(trip);
                addHistory(trip, "CANCELLED", "user:" + currentUser.getUsername(), currentUser.getFullName());
                log.info("❌ Рейс {} отменен администратором", trip.getId());
                return ResponseEntity.ok(trip);
            }

            if (newStatus == TripStatus.DELETED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.DELETED);
                tripRepository.save(trip);
                addHistory(trip, "DELETED", "user:" + currentUser.getUsername(), currentUser.getFullName());
                log.info("🗑️ Рейс {} удален администратором", trip.getId());
                return ResponseEntity.ok(trip);
            }

            if (newStatus == TripStatus.LOADED && trip.getStatus() != TripStatus.LOADED) {
                log.info("🚛 АДМИН переводит рейс {} в LOADED", trip.getId());

                trip.setStatus(TripStatus.LOADED);
                tripRepository.save(trip);
                addHistory(trip, "LOADED", "user:" + currentUser.getUsername(), currentUser.getFullName());

                externalSystemStub.sendToDispatchSystem(trip);

                log.info("✅ Рейс {} переведен в LOADED", trip.getId());
                return ResponseEntity.ok(trip);
            }

            trip.setStatus(newStatus);
            tripRepository.save(trip);
            addHistory(trip, newStatusStr, "user:" + currentUser.getUsername(), currentUser.getFullName());
            log.info("✅ Рейс {} изменен на {} администратором", trip.getId(), newStatus);

            Request request = trip.getRequest();
            boolean allProcessed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .allMatch(t -> t.getStatus() == TripStatus.PROCESSED);

            if (allProcessed) {
                request.setStatus(RequestStatus.PROCESSED);
                requestRepository.save(request);
                log.info("✅ Все рейсы заявки {} обработаны", request.getId());
            }

            return ResponseEntity.ok(trip);
        }

        if (currentUser.getRole() == Role.DISPATCHER) {
            if (newStatus == TripStatus.LOADED) {
                log.warn("⚠️ Диспетчер {} пытается установить статус LOADED для рейса {} - доступ запрещен",
                        currentUser.getUsername(), trip.getId());
                return ResponseEntity.status(403).body("Диспетчер не может установить статус 'Погружен'");
            }

            if (newStatus == TripStatus.CANCELLED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.CANCELLED);
                tripRepository.save(trip);
                addHistory(trip, "CANCELLED", "user:" + currentUser.getUsername(), currentUser.getFullName());
                log.info("❌ Рейс {} отменен диспетчером", trip.getId());
                return ResponseEntity.ok(trip);
            }

            if (newStatus == TripStatus.DELETED && trip.getStatus() == TripStatus.NEW) {
                trip.setStatus(TripStatus.DELETED);
                tripRepository.save(trip);
                addHistory(trip, "DELETED", "user:" + currentUser.getUsername(), currentUser.getFullName());
                log.info("🗑️ Рейс {} удален диспетчером", trip.getId());
                return ResponseEntity.ok(trip);
            }

            trip.setStatus(newStatus);
            tripRepository.save(trip);
            addHistory(trip, newStatusStr, "user:" + currentUser.getUsername(), currentUser.getFullName());
            log.info("✅ Рейс {} изменен на {} диспетчером", trip.getId(), newStatus);

            Request request = trip.getRequest();
            boolean allProcessed = request.getTrips().stream()
                    .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                    .allMatch(t -> t.getStatus() == TripStatus.PROCESSED);

            if (allProcessed) {
                request.setStatus(RequestStatus.PROCESSED);
                requestRepository.save(request);
                log.info("✅ Все рейсы заявки {} обработаны", request.getId());
            }

            return ResponseEntity.ok(trip);
        }

        return ResponseEntity.status(403).body("Доступ запрещен");
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<?> syncWithDispatch(@PathVariable Long id) {
        Trip trip = tripRepository.findById(id).orElse(null);
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }

        if (!trip.getSyncedToDispatch()) {
            boolean sent = dispatchSyncService.sendTripToDispatch(trip);
            if (sent) {
                trip.setSyncedToDispatch(true);
                trip.setSyncedAt(LocalDateTime.now());
                tripRepository.save(trip);
                addHistory(trip, "SYNCED_TO_DISPATCH", "system", "Система диспетчеризации");
                return ResponseEntity.ok(Map.of("success", true, "message", "Рейс синхронизирован с системой диспетчеризации"));
            } else {
                return ResponseEntity.status(500).body(Map.of("success", false, "message", "Ошибка синхронизации с системой диспетчеризации"));
            }
        }

        return ResponseEntity.ok(Map.of("success", false, "message", "Рейс уже синхронизирован"));
    }

    @PostMapping("/dispatch/update-status")
    public ResponseEntity<?> updateStatusFromDispatch(@RequestBody Map<String, Object> data) {
        log.info("=== ОБНОВЛЕНИЕ СТАТУСА ИЗ ДИСПЕТЧЕРИЗАЦИИ ===");
        log.info("Получены данные: {}", data);

        try {
            Long tripId = Long.parseLong(data.get("tripId").toString());
            String statusName = data.get("statusName") != null ? data.get("statusName").toString() : "";
            Long statusId = Long.parseLong(data.get("statusId").toString());

            log.info("Рейс ID: {}, Статус: {}, StatusId: {}", tripId, statusName, statusId);

            boolean updated = syncService.updateTripStatus(tripId, statusName, statusId);

            if (updated) {
                log.info("✅ Статус рейса {} успешно обновлен", tripId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Статус обновлен",
                        "tripId", tripId,
                        "newStatus", statusName
                ));
            } else {
                log.warn("❌ Не удалось обновить статус рейса {}", tripId);
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Не удалось обновить статус. Проверьте сопоставление статусов.",
                        "tripId", tripId
                ));
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
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
            trip.setStatus(TripStatus.DELETED);
            tripRepository.save(trip);
            addHistory(trip, "DELETED", "user:" + currentUser.getUsername(), currentUser.getFullName());
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно. Рейс не в статусе 'Новый' или недостаточно прав.");
    }
}