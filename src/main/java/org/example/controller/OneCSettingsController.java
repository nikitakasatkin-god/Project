package org.example.controller;

import org.example.config.IntegrationSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sync/onec")
public class OneCSettingsController {

    private final IntegrationSettings settings;

    public OneCSettingsController(IntegrationSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> response = new HashMap<>();
        response.put("integrationEnabled", settings.isOnecIntegrationEnabled());
        response.put("apiUrl", settings.getOnecApiUrl());
        response.put("username", settings.getOnecUsername());
        response.put("syncInterval", settings.getSyncInterval() / 1000);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> newSettings) {
        if (newSettings.containsKey("integrationEnabled")) {
            boolean enabled = (Boolean) newSettings.get("integrationEnabled");
            settings.setOnecIntegrationEnabled(enabled);
            System.setProperty("onec.integration.enabled", String.valueOf(enabled));
        }
        if (newSettings.containsKey("apiUrl")) {
            String url = (String) newSettings.get("apiUrl");
            settings.setOnecApiUrl(url);
            System.setProperty("onec.api.url", url);
        }
        if (newSettings.containsKey("username")) {
            String username = (String) newSettings.get("username");
            settings.setOnecUsername(username);
            System.setProperty("onec.api.username", username);
        }
        if (newSettings.containsKey("password")) {
            String password = (String) newSettings.get("password");
            settings.setOnecPassword(password);
            System.setProperty("onec.api.password", password);
        }
        if (newSettings.containsKey("syncInterval")) {
            long intervalSeconds = Long.parseLong(newSettings.get("syncInterval").toString());
            settings.setSyncInterval(intervalSeconds * 1000);
            System.setProperty("onec.sync.interval", String.valueOf(intervalSeconds * 1000));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Настройки 1С сохранены");
        response.put("settings", getSettings().getBody());
        return ResponseEntity.ok(response);
    }
}