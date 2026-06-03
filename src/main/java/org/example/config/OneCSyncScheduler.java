package org.example.config;

import org.example.service.OneCIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class OneCSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(OneCSyncScheduler.class);
    private final OneCIntegrationService integrationService;

    @Value("${onec.integration.enabled:false}")
    private boolean integrationEnabled;

    @Value("${onec.sync.interval:60000}")
    private long syncInterval;

    public OneCSyncScheduler(OneCIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Scheduled(fixedDelayString = "${onec.sync.interval:60000}", initialDelay = 10000)
    public void scheduledSync() {
        if (!integrationEnabled) {
            log.debug("Интеграция с 1С отключена");
            return;
        }

        log.info("=== АВТОМАТИЧЕСКАЯ СИНХРОНИЗАЦИЯ С 1С (интервал: {} мс) ===", syncInterval);
        integrationService.syncAllDirectories();
    }
}