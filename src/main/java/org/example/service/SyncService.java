package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.example.repository.DispatchStatusMappingRepository;
import org.example.repository.TripHistoryRepository;
import org.example.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    private final TripHistoryRepository tripHistoryRepository;
    private final DispatchStatusMappingRepository dispatchStatusMappingRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dispatch.system.url:http://localhost:8081}")
    private String dispatchSystemUrl;

    @Value("${dispatch.system.username:admin}")
    private String dispatchUsername;

    @Value("${dispatch.system.password:admin123}")
    private String dispatchPassword;

    @Value("${sync.send.batch.size:10}")
    private int sendBatchSize;

    public SyncService(TripRepository tripRepository,
                       TripHistoryRepository tripHistoryRepository,
                       DispatchStatusMappingRepository dispatchStatusMappingRepository) {
        this.tripRepository = tripRepository;
        this.tripHistoryRepository = tripHistoryRepository;
        this.dispatchStatusMappingRepository = dispatchStatusMappingRepository;
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

        int totalSent = 0;
        for (int i = 0; i < tripsToSync.size(); i += sendBatchSize) {
            int end = Math.min(i + sendBatchSize, tripsToSync.size());
            List<Trip> batch = tripsToSync.subList(i, end);

            int sent = sendBatchToDispatch(batch);
            totalSent += sent;

            if (sent > 0) {
                log.info("Отправлена партия {} из {} рейсов", (i / sendBatchSize) + 1, sent);
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
            log.info("Запрос к URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            String auth = dispatchUsername + ":" + dispatchPassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Ответ от диспетчеризации: статус={}", response.getStatusCode());
            log.info("Тело ответа: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    List<Map<String, Object>> updates = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
                    log.info("Получено обновлений статусов (массив): {}", updates.size());

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
                } catch (Exception e) {
                    try {
                        Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
                        if (responseMap.containsKey("updates")) {
                            List<Map<String, Object>> updates = (List<Map<String, Object>>) responseMap.get("updates");
                            log.info("Получено обновлений статусов (поле updates): {}", updates.size());

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
                                } catch (Exception ex) {
                                    log.error("Ошибка обработки обновления: {}", ex.getMessage());
                                }
                            }
                            return updatedCount;
                        } else if (responseMap.containsKey("count")) {
                            int count = Integer.parseInt(responseMap.get("count").toString());
                            log.info("Получено обновлений: {}", count);
                            return count;
                        }
                    } catch (Exception ex) {
                        log.error("Ошибка парсинга ответа: {}", ex.getMessage());
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Ошибка при получении статусов из диспетчеризации: {}", e.getMessage(), e);
            return -1;
        }
    }

    public boolean updateTripStatusFromDispatch(Long tripId, String dispatchStatusName, Long dispatchStatusId) {
        log.info("=== ОБНОВЛЕНИЕ СТАТУСА РЕЙСА {} ИЗ ДИСПЕТЧЕРИЗАЦИИ ===", tripId);
        log.info("Статус из диспетчеризации: name={}, id={}", dispatchStatusName, dispatchStatusId);

        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("Рейс с ID {} не найден", tripId);
            return false;
        }

        log.info("Текущий статус рейса {}: {}", tripId, trip.getStatus());

        DispatchStatusMapping mapping = dispatchStatusMappingRepository.findByDispatchStatusId(dispatchStatusId).orElse(null);

        if (mapping == null) {
            log.warn("Не найдено сопоставление для статуса диспетчеризации: id={}, name={}", dispatchStatusId, dispatchStatusName);
            return false;
        }

        log.info("Найдено сопоставление: статус диспетчеризации {} -> локальный статус {}",
                dispatchStatusId, mapping.getLocalStatus().getCode());

        if (mapping.getLocalStatus() == null) {
            log.warn("Локальный статус в сопоставлении пуст");
            return false;
        }

        try {
            TripStatus newStatus = TripStatus.valueOf(mapping.getLocalStatus().getCode());
            TripStatus oldStatus = trip.getStatus();

            String newStatusDisplay = getStatusDisplayName(newStatus);
            String localStatusCode = mapping.getLocalStatus().getCode();

            log.info("Преобразование: {} -> {} (отображается как: {})",
                    mapping.getLocalStatus().getCode(), newStatus, newStatusDisplay);

            TripHistory history = new TripHistory();
            history.setTrip(trip);
            history.setStatusCode(localStatusCode);
            history.setStatusDisplay(newStatusDisplay);
            history.setChangedBy("dispatch_system");
            history.setUserName("Система диспетчеризации");
            history.setDispatchStatusName(dispatchStatusName);
            history.setFromDispatch(true);
            history.setStatus("STATUS_UPDATED_FROM_DISPATCH");

            tripHistoryRepository.save(history);

            if (oldStatus != newStatus) {
                trip.setStatus(newStatus);
                trip.setDispatchStatusId(dispatchStatusId);
                trip.setDispatchStatusName(dispatchStatusName);
                tripRepository.save(trip);

                log.info("✅ Статус рейса {} обновлен: {} -> {} (из диспетчеризации)",
                        tripId, oldStatus, newStatus);
                return true;
            } else {
                log.info("Статус рейса {} уже {}, но история сохранена", tripId, newStatus);
                return true;
            }

        } catch (IllegalArgumentException e) {
            log.error("Неизвестный локальный статус: {}", mapping.getLocalStatus().getCode(), e);
            return false;
        }
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
        log.info("=== ПОЛУЧЕНИЕ СПИСКА СТАТУСОВ ИЗ ДИСПЕТЧЕРИЗАЦИИ ===");

        try {
            String url = dispatchSystemUrl + "/api/statuses";
            log.info("Запрос к URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            String auth = dispatchUsername + ":" + dispatchPassword;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    List.class
            );

            log.info("Ответ от диспетчеризации: статус={}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> statuses = response.getBody();
                log.info("Получено {} статусов из диспетчеризации", statuses.size());
                return statuses;
            } else {
                log.error("Ошибка при получении статусов: {}", response.getStatusCode());
                return List.of();
            }

        } catch (Exception e) {
            log.error("Ошибка при получении статусов из диспетчеризации: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public boolean updateTripStatus(Long tripId, String dispatchStatus, Long dispatchStatusId) {
        return updateTripStatusFromDispatch(tripId, dispatchStatus, dispatchStatusId);
    }
}