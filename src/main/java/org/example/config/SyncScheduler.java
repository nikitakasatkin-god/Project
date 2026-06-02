package org.example.config;

import org.example.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);
    private final SyncService syncService;

    @Value("${sync.interval:60000}")
    private long syncIntervalMs;

    @Value("${sync.auto.enabled:true}")
    private boolean autoSyncEnabled;

    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedDelayString = "${sync.interval:60000}", initialDelay = 10000)
    public void scheduledSync() {
        if (!autoSyncEnabled) {
            log.debug("Автоматическая синхронизация отключена");
            return;
        }

        log.info("=== АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ (интервал: {} сек) ===", syncIntervalMs / 1000);

        int sentCount = syncService.sendTripsToDispatch();

        if (sentCount > 0) {
            log.info("✅ Отправлено {} рейсов в систему диспетчеризации", sentCount);
        } else if (sentCount == 0) {
            log.info("Нет рейсов для отправки");
        } else {
            log.error("❌ Ошибка при отправке рейсов");
        }

        int receivedCount = syncService.receiveStatusesFromDispatch();

        if (receivedCount > 0) {
            log.info("✅ Получено {} обновлений статусов из диспетчеризации", receivedCount);
        }
    }
}