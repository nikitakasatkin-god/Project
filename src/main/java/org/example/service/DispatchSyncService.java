package org.example.service;

import org.example.client.DispatchApiClient; // ДОБАВЛЕНО
import org.example.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DispatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(DispatchSyncService.class);

    private final DispatchApiClient dispatchApiClient; // ДОБАВЛЕНО

    public DispatchSyncService(DispatchApiClient dispatchApiClient) { // ДОБАВЛЕНО
        this.dispatchApiClient = dispatchApiClient;
    }

    public boolean sendTripToDispatch(Trip trip) {
        log.info("=== ОТПРАВКА РЕЙСА {} В ДИСПЕТЧЕРИЗАЦИЮ ===", trip.getId());

        try {
            Map<String, Object> tripData = new HashMap<>();
            tripData.put("id", trip.getId());
            tripData.put("requestId", trip.getRequest().getId());
            tripData.put("carrierName", trip.getCarrier() != null ? trip.getCarrier().getName() : "");
            tripData.put("vehiclePlate", trip.getVehiclePlate() != null ? trip.getVehiclePlate() : "");
            tripData.put("trailerPlate", trip.getTrailerPlate() != null ? trip.getTrailerPlate() : "");
            tripData.put("vehicleBrand", trip.getVehicleBrand() != null ? trip.getVehicleBrand() : "");
            tripData.put("driverName", trip.getDriverName() != null ? trip.getDriverName() : "");
            tripData.put("tripDate", trip.getTripDate() != null ? trip.getTripDate().toString() : "");
            tripData.put("volume", trip.getVolume());
            tripData.put("status", trip.getStatus().name());

            log.info("Данные для отправки: id={}, requestId={}, carrier={}",
                    trip.getId(), trip.getRequest().getId(), trip.getCarrier().getName());

            // Используем API-клиент
            boolean sent = dispatchApiClient.sendTripsToDispatch(java.util.List.of(tripData));

            if (sent) {
                trip.setSyncedToDispatch(true);
                trip.setSyncedAt(LocalDateTime.now());
                log.info("✅ Рейс {} синхронизирован с системой диспетчеризации", trip.getId());
            }

            return sent;

        } catch (Exception e) {
            log.error("ОШИБКА при отправке рейса {}: {}", trip.getId(), e.getMessage(), e);
            return false;
        }
    }
}