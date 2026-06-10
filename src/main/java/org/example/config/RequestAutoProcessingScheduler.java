package org.example.config;

import org.example.service.RequestAutoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RequestAutoProcessingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RequestAutoProcessingScheduler.class);

    private final RequestAutoProcessingService processingService;
    private final RequestProcessingSettings settings;

    public RequestAutoProcessingScheduler(RequestAutoProcessingService processingService,
                                          RequestProcessingSettings settings) {
        this.processingService = processingService;
        this.settings = settings;
    }

    /**
     * Проверка и автоматическая обработка заявок каждые 30 минут
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 300000)
    public void autoProcessRequests() {
        if (settings.isAutoProcessEnabled()) {
            log.info("=== АВТОМАТИЧЕСКАЯ ОБРАБОТКА ЗАЯВОК ===");
            int processed = processingService.autoProcessAllReadyRequests();
            if (processed > 0) {
                log.info("Обработано заявок: {}", processed);
            }
        }
    }

    /**
     * Проверка и автоматическое завершение заявок каждые 30 минут
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 600000)
    public void autoCompleteRequests() {
        if (settings.isAutoCompleteEnabled()) {
            log.info("=== АВТОМАТИЧЕСКОЕ ЗАВЕРШЕНИЕ ЗАЯВОК ===");
            int completed = processingService.autoCompleteAllReadyRequests();
            if (completed > 0) {
                log.info("Завершено заявок: {}", completed);
            }
        }
    }
}