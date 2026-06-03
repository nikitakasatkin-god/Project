package org.example.controller;

import org.example.service.SyncService;
import org.example.service.SyncSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);
    private final SyncService syncService;
    private final SyncSettingsService syncSettingsService;

    public SyncController(SyncService syncService, SyncSettingsService syncSettingsService) {
        this.syncService = syncService;
        this.syncSettingsService = syncSettingsService;
    }

    @PostMapping("/send-trips")
    public ResponseEntity<Map<String, Object>> sendTrips() {
        log.info("POST /api/sync/send-trips - ручная отправка рейсов");
        int count = syncService.sendTripsToDispatch();
        Map<String, Object> response = new HashMap<>();
        response.put("success", count >= 0);
        response.put("count", count);
        if (count < 0) {
            response.put("message", "Ошибка при отправке рейсов в систему диспетчеризации");
        } else {
            response.put("message", "Отправлено рейсов: " + count);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/receive-statuses")
    public ResponseEntity<Map<String, Object>> receiveStatuses() {
        log.info("POST /api/sync/receive-statuses - ручное получение статусов");
        int count = syncService.receiveStatusesFromDispatch();
        Map<String, Object> response = new HashMap<>();
        response.put("success", count >= 0);
        response.put("count", count);
        if (count < 0) {
            response.put("message", "Ошибка при получении статусов из системы диспетчеризации");
        } else {
            response.put("message", "Получено обновлений статусов: " + count);
        }
        log.info("Ответ: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSyncSettings() {
        log.info("GET /api/sync/settings - получение настроек");
        return ResponseEntity.ok(syncSettingsService.getSettings());
    }

    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSyncSettings(@RequestBody Map<String, Object> settings) {
        log.info("POST /api/sync/settings - обновление настроек: {}", settings);
        Map<String, Object> result = syncSettingsService.updateSettings(settings);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dispatch-statuses")
    public ResponseEntity<List<Map<String, Object>>> getDispatchStatuses() {
        log.info("GET /api/sync/dispatch-statuses - запрос статусов из диспетчеризации");
        List<Map<String, Object>> statuses = syncService.fetchDispatchStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        log.info("Health check запрос от системы диспетчеризации");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}