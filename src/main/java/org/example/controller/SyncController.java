package org.example.controller;

import org.example.service.SyncService;
import org.example.service.SyncSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
        int count = syncService.receiveStatusesFromDispatch();
        Map<String, Object> response = new HashMap<>();
        response.put("success", count >= 0);
        response.put("count", count);
        if (count < 0) {
            response.put("message", "Ошибка при получении статусов из системы диспетчеризации");
        } else {
            response.put("message", "Получено обновлений статусов: " + count);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSyncSettings() {
        return ResponseEntity.ok(syncSettingsService.getSettings());
    }

    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSyncSettings(@RequestBody Map<String, Object> newSettings) {
        log.info("POST /api/sync/settings - обновление настроек");
        Map<String, Object> result = syncSettingsService.updateSettings(newSettings);
        return ResponseEntity.ok(result);
    }
}