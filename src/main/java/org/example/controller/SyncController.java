package org.example.controller;

import org.example.client.DispatchApiClient;
import org.example.config.SyncScheduler;
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
    private final DispatchApiClient dispatchApiClient;
    private final SyncScheduler syncScheduler;

    public SyncController(SyncService syncService,
                          SyncSettingsService syncSettingsService,
                          DispatchApiClient dispatchApiClient,
                          SyncScheduler syncScheduler) {
        this.syncService = syncService;
        this.syncSettingsService = syncSettingsService;
        this.dispatchApiClient = dispatchApiClient;
        this.syncScheduler = syncScheduler;
    }

    @PostMapping("/send-trips")
    public ResponseEntity<Map<String, Object>> sendTrips() {
        log.info("POST /api/sync/send-trips - ручная отправка рейсов");
        int count = syncScheduler.manualSend();
        Map<String, Object> response = new HashMap<>();
        response.put("success", count >= 0);
        response.put("count", count);
        response.put("message", count >= 0 ? "Отправлено рейсов: " + count : "Ошибка при отправке");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/receive-statuses")
    public ResponseEntity<Map<String, Object>> receiveStatuses() {
        log.info("POST /api/sync/receive-statuses - ручное получение статусов");
        int count = syncScheduler.manualReceive();
        Map<String, Object> response = new HashMap<>();
        response.put("success", count >= 0);
        response.put("count", count);
        response.put("message", count >= 0 ? "Получено обновлений: " + count : "Ошибка при получении");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSyncSettings() {
        log.info("GET /api/sync/settings - получение настроек");
        Map<String, Object> settings = syncSettingsService.getSettings();

        // ✅ Добавляем информацию о статусе планировщиков
        settings.put("primarySchedulerAlive", syncSettingsService.isAlive());
        settings.put("schedulerMode", syncSettingsService.isAlive() ? "PRIMARY" : "FALLBACK");

        return ResponseEntity.ok(settings);
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
        boolean isAvailable = dispatchApiClient.isDispatchAvailable();
        if (!isAvailable) {
            log.warn("⚠️ Диспетчеризация недоступна");
            return ResponseEntity.ok(List.of());
        }
        List<Map<String, Object>> statuses = syncService.fetchDispatchStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        boolean isDispatchAvailable = dispatchApiClient.isDispatchAvailable();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "dispatchAvailable", isDispatchAvailable,
                "primarySchedulerAlive", syncSettingsService.isAlive()
        ));
    }
}