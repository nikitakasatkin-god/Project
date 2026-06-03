package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private ScheduledFuture<?> scheduledSendTask;
    private ScheduledFuture<?> scheduledReceiveTask;

    private long sendIntervalMs = 60000;
    private boolean sendEnabled = true;
    private int sendBatchSize = 10;

    private long receiveIntervalMs = 60000;
    private boolean receiveEnabled = true;

    public SyncSettingsService(SyncService syncService) {
        this.syncService = syncService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(2);
        this.taskScheduler.setThreadNamePrefix("sync-scheduler-");
        this.taskScheduler.initialize();
    }

    @PostConstruct
    public void init() {
        // Загружаем настройки из System properties
        try {
            this.sendIntervalMs = Long.parseLong(System.getProperty("sync.send.interval", "60000"));
        } catch (NumberFormatException e) {
            this.sendIntervalMs = 60000;
        }
        this.sendEnabled = Boolean.parseBoolean(System.getProperty("sync.send.enabled", "true"));
        this.sendBatchSize = Integer.parseInt(System.getProperty("sync.send.batch.size", "10"));

        try {
            this.receiveIntervalMs = Long.parseLong(System.getProperty("sync.receive.interval", "60000"));
        } catch (NumberFormatException e) {
            this.receiveIntervalMs = 60000;
        }
        this.receiveEnabled = Boolean.parseBoolean(System.getProperty("sync.receive.enabled", "true"));

        log.info("Инициализация сервиса настроек синхронизации");
        log.info("Отправка: интервал={} мс ({} сек), включена={}, размер пакета={}",
                sendIntervalMs, sendIntervalMs / 1000, sendEnabled, sendBatchSize);
        log.info("Получение: интервал={} мс ({} сек), включено={}",
                receiveIntervalMs, receiveIntervalMs / 1000, receiveEnabled);

        startSchedulers();
    }

    @PreDestroy
    public void destroy() {
        stopSchedulers();
        taskScheduler.destroy();
    }

    public void startSchedulers() {
        startSendScheduler();
        startReceiveScheduler();
    }

    public void stopSchedulers() {
        stopSendScheduler();
        stopReceiveScheduler();
    }

    // ==================== ОТПРАВКА ====================

    public void startSendScheduler() {
        if (scheduledSendTask != null && !scheduledSendTask.isCancelled()) {
            stopSendScheduler();
        }

        if (!sendEnabled) {
            log.info("Отправка рейсов отключена");
            return;
        }

        scheduledSendTask = taskScheduler.scheduleWithFixedDelay(() -> {
            try {
                log.info("=== АВТОМАТИЧЕСКАЯ ОТПРАВКА РЕЙСОВ (интервал: {} сек) ===", sendIntervalMs / 1000);
                int sentCount = syncService.sendTripsToDispatch();
                if (sentCount > 0) {
                    log.info("✅ Отправлено {} рейсов в систему диспетчеризации", sentCount);
                } else if (sentCount == 0) {
                    log.debug("Нет рейсов для отправки");
                } else {
                    log.error("❌ Ошибка при отправке рейсов");
                }
            } catch (Exception e) {
                log.error("Ошибка при отправке: {}", e.getMessage(), e);
            }
        }, sendIntervalMs);

        log.info("Планировщик отправки запущен с интервалом {} мс ({} сек)", sendIntervalMs, sendIntervalMs / 1000);
    }

    public void stopSendScheduler() {
        if (scheduledSendTask != null) {
            scheduledSendTask.cancel(false);
            scheduledSendTask = null;
            log.info("Планировщик отправки остановлен");
        }
    }

    public void restartSendScheduler() {
        log.info("Перезапуск планировщика отправки с новыми настройками");
        stopSendScheduler();
        startSendScheduler();
    }

    // ==================== ПОЛУЧЕНИЕ ====================

    public void startReceiveScheduler() {
        if (scheduledReceiveTask != null && !scheduledReceiveTask.isCancelled()) {
            stopReceiveScheduler();
        }

        if (!receiveEnabled) {
            log.info("Получение статусов отключено");
            return;
        }

        scheduledReceiveTask = taskScheduler.scheduleWithFixedDelay(() -> {
            try {
                log.info("=== АВТОМАТИЧЕСКОЕ ПОЛУЧЕНИЕ СТАТУСОВ (интервал: {} сек) ===", receiveIntervalMs / 1000);
                int receivedCount = syncService.receiveStatusesFromDispatch();
                if (receivedCount > 0) {
                    log.info("✅ Получено {} обновлений статусов из диспетчеризации", receivedCount);
                } else if (receivedCount == 0) {
                    log.debug("Нет обновлений статусов");
                } else if (receivedCount < 0) {
                    log.error("❌ Ошибка при получении статусов");
                }
            } catch (Exception e) {
                log.error("Ошибка при получении статусов: {}", e.getMessage(), e);
            }
        }, receiveIntervalMs);

        log.info("Планировщик получения запущен с интервалом {} мс ({} сек)", receiveIntervalMs, receiveIntervalMs / 1000);
    }

    public void stopReceiveScheduler() {
        if (scheduledReceiveTask != null) {
            scheduledReceiveTask.cancel(false);
            scheduledReceiveTask = null;
            log.info("Планировщик получения остановлен");
        }
    }

    public void restartReceiveScheduler() {
        log.info("Перезапуск планировщика получения с новыми настройками");
        stopReceiveScheduler();
        startReceiveScheduler();
    }

    // ==================== ОБНОВЛЕНИЕ НАСТРОЕК ====================

    public Map<String, Object> updateSettings(Map<String, Object> newSettings) {
        log.info("Обновление настроек синхронизации: {}", newSettings);

        boolean needRestartSend = false;
        boolean needRestartReceive = false;

        if (newSettings.containsKey("sendIntervalSeconds")) {
            long newIntervalSeconds = Long.parseLong(newSettings.get("sendIntervalSeconds").toString());
            long newIntervalMs = newIntervalSeconds * 1000;
            if (newIntervalMs != sendIntervalMs && newIntervalMs >= 10000) {
                this.sendIntervalMs = newIntervalMs;
                System.setProperty("sync.send.interval", String.valueOf(newIntervalMs));
                needRestartSend = true;
                log.info("Интервал отправки изменен с {} на {} секунд", sendIntervalMs / 1000, newIntervalSeconds);
            }
        }

        if (newSettings.containsKey("sendEnabled")) {
            boolean newEnabled = Boolean.parseBoolean(newSettings.get("sendEnabled").toString());
            if (newEnabled != sendEnabled) {
                this.sendEnabled = newEnabled;
                System.setProperty("sync.send.enabled", String.valueOf(newEnabled));
                needRestartSend = true;
                log.info("Отправка изменена на: {}", newEnabled ? "Включена" : "Отключена");
            }
        }

        if (newSettings.containsKey("sendBatchSize")) {
            int newBatchSize = Integer.parseInt(newSettings.get("sendBatchSize").toString());
            if (newBatchSize != sendBatchSize && newBatchSize >= 1 && newBatchSize <= 50) {
                this.sendBatchSize = newBatchSize;
                System.setProperty("sync.send.batch.size", String.valueOf(newBatchSize));
                log.info("Размер пакета отправки изменен на: {}", newBatchSize);
            }
        }

        if (newSettings.containsKey("receiveIntervalSeconds")) {
            long newIntervalSeconds = Long.parseLong(newSettings.get("receiveIntervalSeconds").toString());
            long newIntervalMs = newIntervalSeconds * 1000;
            if (newIntervalMs != receiveIntervalMs && newIntervalMs >= 10000) {
                this.receiveIntervalMs = newIntervalMs;
                System.setProperty("sync.receive.interval", String.valueOf(newIntervalMs));
                needRestartReceive = true;
                log.info("Интервал получения изменен с {} на {} секунд", receiveIntervalMs / 1000, newIntervalSeconds);
            }
        }

        if (newSettings.containsKey("receiveEnabled")) {
            boolean newEnabled = Boolean.parseBoolean(newSettings.get("receiveEnabled").toString());
            if (newEnabled != receiveEnabled) {
                this.receiveEnabled = newEnabled;
                System.setProperty("sync.receive.enabled", String.valueOf(newEnabled));
                needRestartReceive = true;
                log.info("Получение изменено на: {}", newEnabled ? "Включено" : "Отключено");
            }
        }

        // Перезапускаем планировщики с новыми настройками
        if (needRestartSend) {
            restartSendScheduler();
        }

        if (needRestartReceive) {
            restartReceiveScheduler();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Настройки синхронизации обновлены");
        response.put("settings", getSettings());

        return response;
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("sendIntervalMs", sendIntervalMs);
        settings.put("sendIntervalSeconds", sendIntervalMs / 1000);
        settings.put("sendEnabled", sendEnabled);
        settings.put("sendBatchSize", sendBatchSize);
        settings.put("receiveIntervalMs", receiveIntervalMs);
        settings.put("receiveIntervalSeconds", receiveIntervalMs / 1000);
        settings.put("receiveEnabled", receiveEnabled);
        return settings;
    }
}