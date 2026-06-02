package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Service
public class SyncSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SyncSettingsService.class);

    private final SyncService syncService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;

    private long syncIntervalMs;
    private boolean autoSyncEnabled;
    private int batchSize;

    public SyncSettingsService(SyncService syncService) {
        this.syncService = syncService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setThreadNamePrefix("sync-scheduler-");
        this.taskScheduler.initialize();
    }

    @PostConstruct
    public void init() {
        // Загружаем настройки из system properties или application.properties
        this.syncIntervalMs = Long.parseLong(System.getProperty("sync.interval", "60000"));
        this.autoSyncEnabled = Boolean.parseBoolean(System.getProperty("sync.auto.enabled", "true"));
        this.batchSize = Integer.parseInt(System.getProperty("sync.batch.size", "10"));

        log.info("Инициализация сервиса синхронизации");
        log.info("Интервал: {} мс ({} сек)", syncIntervalMs, syncIntervalMs / 1000);
        log.info("Автосинхронизация: {}", autoSyncEnabled);
        log.info("Размер пакета: {}", batchSize);

        startScheduler();
    }

    @PreDestroy
    public void destroy() {
        stopScheduler();
        taskScheduler.destroy();
    }

    public void startScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            stopScheduler();
        }

        if (!autoSyncEnabled) {
            log.info("Автоматическая синхронизация отключена");
            return;
        }

        scheduledTask = taskScheduler.scheduleWithFixedDelay(() -> {
            try {
                log.info("=== АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ (интервал: {} сек) ===", syncIntervalMs / 1000);

                int sentCount = syncService.sendTripsToDispatch();
                if (sentCount > 0) {
                    log.info("✅ Отправлено {} рейсов в систему диспетчеризации", sentCount);
                } else if (sentCount == 0) {
                    log.debug("Нет рейсов для отправки");
                } else {
                    log.error("❌ Ошибка при отправке рейсов");
                }

                int receivedCount = syncService.receiveStatusesFromDispatch();
                if (receivedCount > 0) {
                    log.info("✅ Получено {} обновлений статусов из диспетчеризации", receivedCount);
                }

            } catch (Exception e) {
                log.error("Ошибка при выполнении синхронизации: {}", e.getMessage(), e);
            }
        }, syncIntervalMs);

        log.info("Планировщик синхронизации запущен с интервалом {} мс", syncIntervalMs);
    }

    public void stopScheduler() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
            log.info("Планировщик синхронизации остановлен");
        }
    }

    public void restartScheduler() {
        log.info("Перезапуск планировщика с новыми настройками");
        stopScheduler();
        startScheduler();
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("syncIntervalMs", syncIntervalMs);
        settings.put("syncIntervalSeconds", syncIntervalMs / 1000);
        settings.put("autoSyncEnabled", autoSyncEnabled);
        settings.put("batchSize", batchSize);
        return settings;
    }

    public Map<String, Object> updateSettings(Map<String, Object> newSettings) {
        log.info("Обновление настроек: {}", newSettings);

        boolean needRestart = false;

        if (newSettings.containsKey("syncIntervalSeconds")) {
            long newIntervalSeconds = Long.parseLong(newSettings.get("syncIntervalSeconds").toString());
            long newIntervalMs = newIntervalSeconds * 1000;
            if (newIntervalMs != syncIntervalMs && newIntervalMs >= 10000) {
                this.syncIntervalMs = newIntervalMs;
                System.setProperty("sync.interval", String.valueOf(newIntervalMs));
                needRestart = true;
                log.info("Интервал синхронизации изменен на {} секунд", newIntervalSeconds);
            }
        }

        if (newSettings.containsKey("autoSyncEnabled")) {
            boolean newAutoSync = Boolean.parseBoolean(newSettings.get("autoSyncEnabled").toString());
            if (newAutoSync != autoSyncEnabled) {
                this.autoSyncEnabled = newAutoSync;
                System.setProperty("sync.auto.enabled", String.valueOf(newAutoSync));
                needRestart = true;
                log.info("Автосинхронизация изменена на: {}", newAutoSync);
            }
        }

        if (newSettings.containsKey("batchSize")) {
            int newBatchSize = Integer.parseInt(newSettings.get("batchSize").toString());
            if (newBatchSize != batchSize && newBatchSize >= 1 && newBatchSize <= 100) {
                this.batchSize = newBatchSize;
                System.setProperty("sync.batch.size", String.valueOf(newBatchSize));
                log.info("Размер пакета изменен на: {}", newBatchSize);
            }
        }

        if (needRestart) {
            restartScheduler();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Настройки синхронизации обновлены" + (needRestart ? " и применены" : ""));
        response.put("settings", getSettings());

        return response;
    }
}