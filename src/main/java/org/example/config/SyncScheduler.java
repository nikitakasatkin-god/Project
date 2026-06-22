package org.example.config;

import org.example.service.SyncService;
import org.example.service.SyncSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncService syncService;
    private final SyncSettingsService syncSettingsService;

    @Value("${sync.send.interval:60000}")
    private long sendIntervalMs;

    @Value("${sync.receive.interval:60000}")
    private long receiveIntervalMs;

    @Value("${sync.send.enabled:true}")
    private boolean sendEnabled;

    @Value("${sync.receive.enabled:true}")
    private boolean receiveEnabled;

    public SyncScheduler(SyncService syncService, SyncSettingsService syncSettingsService) {
        this.syncService = syncService;
        this.syncSettingsService = syncSettingsService;
    }

    /**
     * ✅ РЕЗЕРВНЫЙ ПЛАНИРОВЩИК ОТПРАВКИ
     * Запускается ТОЛЬКО если основной планировщик упал
     */
    @Scheduled(fixedDelayString = "${sync.send.interval:60000}", initialDelay = 30000)
    public void scheduledSend() {
        // ✅ Проверяем: жив ли основной планировщик?
        if (syncSettingsService.isAlive() && syncSettingsService.isSendEnabled()) {
            log.debug("Основной планировщик отправки работает, пропускаем резервный");
            return;
        }

        if (!sendEnabled) {
            log.debug("Отправка рейсов отключена");
            return;
        }

        log.warn("⚠️ РЕЗЕРВНЫЙ ПЛАНИРОВЩИК: Отправка рейсов (основной упал)");
        log.info("=== ОТПРАВКА РЕЙСОВ В ДИСПЕТЧЕРИЗАЦИЮ (интервал: {} сек) ===", sendIntervalMs / 1000);

        try {
            int sentCount = syncService.sendTripsToDispatch();
            if (sentCount > 0) {
                log.info("✅ Отправлено {} рейсов в систему диспетчеризации (резервный)", sentCount);
            } else if (sentCount == 0) {
                log.debug("Нет рейсов для отправки");
            } else {
                log.error("❌ Ошибка при отправке рейсов");
            }
        } catch (Exception e) {
            log.error("❌ Ошибка в резервном планировщике: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ РЕЗЕРВНЫЙ ПЛАНИРОВЩИК ПОЛУЧЕНИЯ
     * Запускается ТОЛЬКО если основной планировщик упал
     */
    @Scheduled(fixedDelayString = "${sync.receive.interval:60000}", initialDelay = 35000)
    public void scheduledReceive() {
        // ✅ Проверяем: жив ли основной планировщик?
        if (syncSettingsService.isAlive() && syncSettingsService.isReceiveEnabled()) {
            log.debug("Основной планировщик получения работает, пропускаем резервный");
            return;
        }

        if (!receiveEnabled) {
            log.debug("Получение статусов отключено");
            return;
        }

        log.warn("⚠️ РЕЗЕРВНЫЙ ПЛАНИРОВЩИК: Получение статусов (основной упал)");
        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ ДИСПЕТЧЕРИЗАЦИИ (интервал: {} сек) ===", receiveIntervalMs / 1000);

        try {
            int receivedCount = syncService.receiveStatusesFromDispatch();
            if (receivedCount > 0) {
                log.info("✅ Получено {} обновлений статусов из диспетчеризации (резервный)", receivedCount);
            } else if (receivedCount == 0) {
                log.debug("Нет обновлений статусов");
            } else if (receivedCount < 0) {
                log.error("❌ Ошибка при получении статусов");
            }
        } catch (Exception e) {
            log.error("❌ Ошибка в резервном планировщике: {}", e.getMessage(), e);
        }
    }

    /**
     * Ручная отправка (для кнопки в интерфейсе)
     */
    public int manualSend() {
        log.info("=== РУЧНАЯ ОТПРАВКА РЕЙСОВ ===");
        return syncService.sendTripsToDispatch();
    }

    /**
     * Ручное получение (для кнопки в интерфейсе)
     */
    public int manualReceive() {
        log.info("=== РУЧНОЕ ПОЛУЧЕНИЕ СТАТУСОВ ===");
        return syncService.receiveStatusesFromDispatch();
    }
}