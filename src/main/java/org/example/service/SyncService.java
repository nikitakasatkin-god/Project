package org.example.service;

import org.example.model.StatusMapping;
import org.example.model.Trip;
import org.example.model.TripStatus;
import org.example.repository.StatusMappingRepository;
import org.example.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final TripRepository tripRepository;
    private final StatusMappingRepository statusMappingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dispatch.system.url:http://localhost:8081}")
    private String dispatchSystemUrl;

    @Value("${dispatch.system.username:admin}")
    private String dispatchUsername;

    @Value("${dispatch.system.password:admin123}")
    private String dispatchPassword;

    @Value("${sync.batch.size:10}")
    private int batchSize;

    public SyncService(TripRepository tripRepository, StatusMappingRepository statusMappingRepository) {
        this.tripRepository = tripRepository;
        this.statusMappingRepository = statusMappingRepository;
    }

    public List<Trip> getTripsReadyForSync() {
        return tripRepository.findAll().stream()
                .filter(t -> t.getStatus() == TripStatus.LOADED && !Boolean.TRUE.equals(t.getSyncedToDispatch()))
                .collect(Collectors.toList());
    }

    public int sendTripsToDispatch() {
        List<Trip> tripsToSync = getTripsReadyForSync();

        log.info("=== ОТПРАВКА РЕЙСОВ В СИСТЕМУ ДИСПЕТЧЕРИЗАЦИИ ===");
        log.info("Найдено рейсов для отправки: {}", tripsToSync.size());

        if (tripsToSync.isEmpty()) {
            return 0;
        }

        int totalSent = 0;
        for (int i = 0; i < tripsToSync.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tripsToSync.size());
            List<Trip> batch = tripsToSync.subList(i, end);

            int sent = sendBatchToDispatch(batch);
            totalSent += sent;

            if (sent > 0) {
                log.info("Отправлена партия {} из {} рейсов", (i / batchSize) + 1, sent);
            }
        }

        return totalSent;
    }

    private int sendBatchToDispatch(List<Trip> trips) {
        if (trips.isEmpty()) {
            return 0;
        }

        try {
            String url = dispatchSystemUrl + "/api/sync/receive-trips";

            List<Map<String, Object>> tripsData = new ArrayList<>();
            for (Trip trip : trips) {
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
                tripsData.add(tripMap);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String auth = dispatchUsername + ":" + dispatchPassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(tripsData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("Ответ от диспетчеризации: статус={}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                for (Trip trip : trips) {
                    trip.setSyncedToDispatch(true);
                    trip.setSyncedAt(LocalDateTime.now());
                    tripRepository.save(trip);
                    log.info("Рейс {} помечен как синхронизированный", trip.getId());
                }
                return trips.size();
            } else {
                log.error("Ошибка при отправке: статус {}", response.getStatusCode());
                return 0;
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке партии рейсов: {}", e.getMessage(), e);
            return 0;
        }
    }

    public int receiveStatusesFromDispatch() {
        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ СИСТЕМЫ ДИСПЕТЧЕРИЗАЦИИ ===");

        try {
            String url = dispatchSystemUrl + "/api/sync/send-statuses";

            HttpHeaders headers = new HttpHeaders();
            String auth = dispatchUsername + ":" + dispatchPassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("count")) {
                Object count = response.getBody().get("count");
                int countInt = 0;
                if (count instanceof Integer) {
                    countInt = (int) count;
                } else if (count instanceof String) {
                    countInt = Integer.parseInt((String) count);
                }
                log.info("Получено обновлений статусов: {}", countInt);
                return countInt;
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статусов: {}", e.getMessage(), e);
        }
        return -1;
    }

    public boolean updateTripStatus(Long tripId, String dispatchStatus, Long dispatchStatusId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("Рейс с ID {} не найден", tripId);
            return false;
        }

        StatusMapping mapping = statusMappingRepository.findByDispatchStatusId(dispatchStatusId).orElse(null);

        if (mapping != null && mapping.getLocalStatus() != null) {
            try {
                TripStatus newStatus = TripStatus.valueOf(mapping.getLocalStatus());
                log.info("Сопоставление статусов: {} -> {}", dispatchStatus, newStatus);
                trip.setStatus(newStatus);
                tripRepository.save(trip);
                log.info("Статус рейса {} обновлен на {}", tripId, newStatus);
                return true;
            } catch (IllegalArgumentException e) {
                log.error("Неизвестный локальный статус: {}", mapping.getLocalStatus());
                return false;
            }
        } else {
            log.warn("Не найдено сопоставление для статуса диспетчеризации: id={}, name={}", dispatchStatusId, dispatchStatus);
            return false;
        }
    }
}