package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.DispatchApiClient;
import org.example.model.*;
import org.example.repository.DispatchStatusMappingRepository;
import org.example.repository.TripHistoryRepository;
import org.example.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final TripRepository tripRepository;
    private final TripHistoryRepository tripHistoryRepository;
    private final DispatchStatusMappingRepository dispatchStatusMappingRepository;
    private final DispatchApiClient dispatchApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyncService(TripRepository tripRepository,
                       TripHistoryRepository tripHistoryRepository,
                       DispatchStatusMappingRepository dispatchStatusMappingRepository,
                       DispatchApiClient dispatchApiClient) {
        this.tripRepository = tripRepository;
        this.tripHistoryRepository = tripHistoryRepository;
        this.dispatchStatusMappingRepository = dispatchStatusMappingRepository;
        this.dispatchApiClient = dispatchApiClient;
    }

    public List<Trip> getTripsReadyForSend() {
        return tripRepository.findAll().stream()
                .filter(t -> t.getStatus() == TripStatus.LOADED && !Boolean.TRUE.equals(t.getSyncedToDispatch()))
                .collect(Collectors.toList());
    }

    public int sendTripsToDispatch() {
        List<Trip> tripsToSync = getTripsReadyForSend();

        log.info("=== ОТПРАВКА РЕЙСОВ В СИСТЕМУ ДИСПЕТЧЕРИЗАЦИИ ===");
        log.info("Найдено рейсов для отправки: {}", tripsToSync.size());

        if (tripsToSync.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> tripsData = new ArrayList<>();
        for (Trip trip : tripsToSync) {
            Map<String, Object> tripMap = new HashMap<>();
            tripMap.put("id", trip.getId());
            tripMap.put("requestId", trip.getRequest().getId());
            tripMap.put("carrierName", trip.getCarrier() != null ? trip.getCarrier().getName() : "");
            tripMap.put("vehiclePlate", trip.getVehiclePlate() != null ? trip.getVehiclePlate() : "");
            tripMap.put("trailerPlate", trip.getTrailerPlate() != null ? trip.getTrailerPlate() : "");
            tripMap.put("vehicleBrand", trip.getVehicleBrand() != null ? trip.getVehicleBrand() : "");
            tripMap.put("driverName", trip.getDriverName() != null ? trip.getDriverName() : "");
            tripMap.put("tripDate", trip.getTripDate() != null ? trip.getTripDate().toString() : "");
            tripMap.put("volume", trip.getVolume());
            tripMap.put("status", trip.getStatus().name());

            // ✅ ДОБАВЛЯЕМ ДАТУ СОЗДАНИЯ В ОТПРАВКУ
            tripMap.put("createdAt", trip.getCreatedAt() != null ? trip.getCreatedAt().toString() : "");
            log.info("Рейс {}: createdAt = '{}'", trip.getId(), trip.getCreatedAt());

            tripsData.add(tripMap);
        }

        boolean success = dispatchApiClient.sendTripsToDispatch(tripsData);

        if (success) {
            for (Trip trip : tripsToSync) {
                trip.setSyncedToDispatch(true);
                trip.setSyncedAt(LocalDateTime.now());
                tripRepository.save(trip);
                log.info("Рейс {} помечен как синхронизированный", trip.getId());
            }
            return tripsToSync.size();
        } else {
            log.error("Ошибка при отправке рейсов в Диспетчеризацию");
            return -1;
        }
    }

    public int receiveStatusesFromDispatch() {
        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ СИСТЕМЫ ДИСПЕТЧЕРИЗАЦИИ ===");

        List<Map<String, Object>> updates = dispatchApiClient.receiveStatusesFromDispatch();

        if (updates.isEmpty()) {
            log.info("Нет обновлений статусов");
            return 0;
        }

        int updatedCount = 0;
        for (Map<String, Object> update : updates) {
            try {
                Long tripId = Long.parseLong(update.get("tripId").toString());
                Long statusId = Long.parseLong(update.get("statusId").toString());
                String statusName = update.get("statusName") != null ? update.get("statusName").toString() : "";

                boolean updated = updateTripStatusFromDispatch(tripId, statusName, statusId);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Ошибка обработки обновления: {}", e.getMessage());
            }
        }

        return updatedCount;
    }

    public boolean updateTripStatusFromDispatch(Long tripId, String dispatchStatusName, Long dispatchStatusId) {
        log.info("=== ОБНОВЛЕНИЕ СТАТУСА РЕЙСА {} ИЗ ДИСПЕТЧЕРИЗАЦИИ ===", tripId);
        log.info("Статус из диспетчеризации: name={}, id={}", dispatchStatusName, dispatchStatusId);

        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("Рейс с ID {} не найден", tripId);
            return false;
        }

        log.info("Текущий статус рейса {}: {}", tripId, trip.getStatusDisplayName());

        DispatchStatusMapping mapping = dispatchStatusMappingRepository.findByDispatchStatusId(dispatchStatusId).orElse(null);

        if (mapping == null) {
            log.warn("Не найдено сопоставление для статуса диспетчеризации: id={}, name={}", dispatchStatusId, dispatchStatusName);
            return false;
        }

        TripStatusEntity newStatusEntity = mapping.getLocalStatus();
        if (newStatusEntity == null) {
            log.warn("Локальный статус в сопоставлении пуст");
            return false;
        }

        log.info("Найдено сопоставление: статус диспетчеризации {} -> локальный статус {} ({})",
                dispatchStatusId, newStatusEntity.getCode(), newStatusEntity.getName());

        TripStatusEntity oldStatusEntity = trip.getStatusEntity();
        String newStatusDisplay = newStatusEntity.getName();

        TripHistory history = new TripHistory();
        history.setTrip(trip);
        history.setStatusCode(newStatusEntity.getCode());
        history.setStatusDisplay(newStatusDisplay);
        history.setChangedBy("dispatch_system");
        history.setUserName("Система диспетчеризации");
        history.setDispatchStatusName(dispatchStatusName);
        history.setFromDispatch(true);
        history.setStatus("STATUS_UPDATED_FROM_DISPATCH");

        tripHistoryRepository.save(history);

        trip.setStatusEntity(newStatusEntity);
        trip.setDispatchStatusId(dispatchStatusId);
        trip.setDispatchStatusName(dispatchStatusName);
        tripRepository.save(trip);

        log.info("✅ Статус рейса {} обновлен: {} -> {} (из диспетчеризации)",
                tripId,
                oldStatusEntity != null ? oldStatusEntity.getName() : "null",
                newStatusDisplay);

        return true;
    }

    private String getStatusDisplayName(TripStatus status) {
        switch (status) {
            case NEW: return "Новый";
            case ARRIVED_LOADING: return "Прибыл на погрузку";
            case LOADED: return "Погружен";
            case IN_TRANSIT: return "В пути";
            case ARRIVED_UNLOADING: return "Прибыл на выгрузку";
            case UNLOADED: return "Выгружен";
            case PROCESSED: return "Обработан";
            case CANCELLED: return "Отменен";
            case DELETED: return "Удален";
            default: return status.name();
        }
    }

    public List<Map<String, Object>> fetchDispatchStatuses() {
        return dispatchApiClient.fetchDispatchStatuses();
    }

    public boolean updateTripStatus(Long tripId, String dispatchStatus, Long dispatchStatusId) {
        return updateTripStatusFromDispatch(tripId, dispatchStatus, dispatchStatusId);
    }
}