package org.example.controller;

import org.example.config.RequestProcessingSettings;
import org.example.service.RequestAutoProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final RequestProcessingSettings processingSettings;
    private final RequestAutoProcessingService processingService;

    public SettingsController(RequestProcessingSettings processingSettings,
                              RequestAutoProcessingService processingService) {
        this.processingSettings = processingSettings;
        this.processingService = processingService;
    }

    @GetMapping("/auto-processing")
    public ResponseEntity<Map<String, Object>> getAutoProcessingSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("autoProcessEnabled", processingSettings.isAutoProcessEnabled());
        settings.put("autoProcessDelayHours", processingSettings.getAutoProcessDelayHours());
        settings.put("autoCompleteEnabled", processingSettings.isAutoCompleteEnabled());
        settings.put("autoCompleteDelayHours", processingSettings.getAutoCompleteDelayHours());
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/auto-processing")
    public ResponseEntity<Map<String, Object>> updateAutoProcessingSettings(@RequestBody Map<String, Object> newSettings) {
        if (newSettings.containsKey("autoProcessEnabled")) {
            boolean enabled = (Boolean) newSettings.get("autoProcessEnabled");
            processingSettings.setAutoProcessEnabled(enabled);
            System.setProperty("request.auto.process.enabled", String.valueOf(enabled));
        }

        if (newSettings.containsKey("autoProcessDelayHours")) {
            double delayHours = Double.parseDouble(newSettings.get("autoProcessDelayHours").toString());
            processingSettings.setAutoProcessDelayHours(delayHours);
            System.setProperty("request.auto.process.delay.hours", String.valueOf(delayHours));
        }

        if (newSettings.containsKey("autoCompleteEnabled")) {
            boolean enabled = (Boolean) newSettings.get("autoCompleteEnabled");
            processingSettings.setAutoCompleteEnabled(enabled);
            System.setProperty("request.auto.complete.enabled", String.valueOf(enabled));
        }

        if (newSettings.containsKey("autoCompleteDelayHours")) {
            double delayHours = Double.parseDouble(newSettings.get("autoCompleteDelayHours").toString());
            processingSettings.setAutoCompleteDelayHours(delayHours);
            System.setProperty("request.auto.complete.delay.hours", String.valueOf(delayHours));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Настройки автоматической обработки заявок сохранены");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/auto-processing/check")
    public ResponseEntity<Map<String, Object>> runManualCheck() {
        int processed = processingService.autoProcessAllReadyRequests();
        int completed = processingService.autoCompleteAllReadyRequests();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("processed", processed);
        response.put("completed", completed);
        response.put("message", String.format("Проверка выполнена. Обработано: %d, Завершено: %d", processed, completed));

        return ResponseEntity.ok(response);
    }
}