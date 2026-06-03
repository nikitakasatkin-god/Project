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

    @Value("${sync.send.interval:60000}")
    private long sendIntervalMs;

    @Value("${sync.receive.interval:60000}")
    private long receiveIntervalMs;

    @Value("${sync.send.enabled:true}")
    private boolean sendEnabled;

    @Value("${sync.receive.enabled:true}")
    private boolean receiveEnabled;

    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    // Планировщик для отправки рейсов в диспетчеризацию
    @Scheduled(fixedDelayString = "${sync.send.interval:60000}", initialDelay = 10000)
    public void scheduledSend() {
        if (!sendEnabled) {
            log.debug("Отправка рейсов отключена");
            return;
        }

        log.info("=== ОТПРАВКА РЕЙСОВ В ДИСПЕТЧЕРИЗАЦИЮ (интервал: {} сек) ===", sendIntervalMs / 1000);

        int sentCount = syncService.sendTripsToDispatch();
        if (sentCount > 0) {
            log.info("✅ Отправлено {} рейсов в систему диспетчеризации", sentCount);
        } else if (sentCount == 0) {
            log.debug("Нет рейсов для отправки");
        } else {
            log.error("❌ Ошибка при отправке рейсов");
        }
    }

    // Планировщик для получения статусов из диспетчеризации
    @Scheduled(fixedDelayString = "${sync.receive.interval:60000}", initialDelay = 15000)
    public void scheduledReceive() {
        if (!receiveEnabled) {
            log.debug("Получение статусов отключено");
            return;
        }

        log.info("=== ПОЛУЧЕНИЕ СТАТУСОВ ИЗ ДИСПЕТЧЕРИЗАЦИИ (интервал: {} сек) ===", receiveIntervalMs / 1000);

        int receivedCount = syncService.receiveStatusesFromDispatch();
        if (receivedCount > 0) {
            log.info("✅ Обновлено {} рейсов из системы диспетчеризации", receivedCount);
        } else if (receivedCount == 0) {
            log.debug("Нет обновлений статусов");
        } else if (receivedCount < 0) {
            log.error("❌ Ошибка при получении статусов");
        }
    }
}