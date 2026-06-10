package org.example.service;

import org.example.model.Request;
import org.example.model.RequestStatus;
import org.example.model.Trip;
import org.example.model.TripStatus;
import org.example.repository.RequestHistoryRepository;
import org.example.repository.RequestRepository;
import org.example.repository.TripRepository;
import org.example.config.RequestProcessingSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestAutoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(RequestAutoProcessingService.class);

    private final RequestRepository requestRepository;
    private final TripRepository tripRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final RequestProcessingSettings settings;

    public RequestAutoProcessingService(RequestRepository requestRepository,
                                        TripRepository tripRepository,
                                        RequestHistoryRepository requestHistoryRepository,
                                        RequestProcessingSettings settings) {
        this.requestRepository = requestRepository;
        this.tripRepository = tripRepository;
        this.requestHistoryRepository = requestHistoryRepository;
        this.settings = settings;
    }

    /**
     * Проверяет, можно ли обработать заявку (перевести в статус PROCESSED)
     */
    public boolean canProcessRequest(Request request) {
        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            log.debug("Заявка {}: статус не 'В работе' (текущий: {})", request.getId(), request.getStatus());
            return false;
        }

        List<Trip> trips = tripRepository.findByRequest(request);

        // Проверяем: Объем заявки = Распределенный объем
        double totalVolume = request.getVolume();
        double assignedVolume = trips.stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .mapToDouble(Trip::getVolume)
                .sum();

        if (Math.abs(totalVolume - assignedVolume) > 0.01) {
            log.debug("Заявка {}: объем не распределен полностью ({} из {} т)",
                    request.getId(), assignedVolume, totalVolume);
            return false;
        }

        // Проверяем: Все активные рейсы имеют статус PROCESSED
        boolean allTripsProcessed = trips.stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .allMatch(t -> t.getStatus() == TripStatus.PROCESSED);

        if (!allTripsProcessed) {
            log.debug("Заявка {}: не все рейсы обработаны", request.getId());
            return false;
        }

        log.info("Заявка {} готова к обработке (объем: {}/{} т, все рейсы обработаны)",
                request.getId(), assignedVolume, totalVolume);
        return true;
    }

    /**
     * Проверяет, можно ли завершить заявку (перевести в статус COMPLETED)
     */
    public boolean canCompleteRequest(Request request) {
        boolean canComplete = request.getStatus() == RequestStatus.PROCESSED;
        if (!canComplete) {
            log.debug("Заявка {}: статус не 'Обработана' (текущий: {})", request.getId(), request.getStatus());
        }
        return canComplete;
    }

    /**
     * Обрабатывает заявку (переводит в статус PROCESSED)
     */
    @Transactional
    public boolean processRequest(Request request, String triggeredBy, String userName) {
        if (!canProcessRequest(request)) {
            log.warn("Заявка {} не может быть обработана", request.getId());
            return false;
        }

        try {
            request.setStatus(RequestStatus.PROCESSED);
            request.setCompletedAt(LocalDateTime.now());
            requestRepository.save(request);

            addHistory(request, RequestStatus.PROCESSED.name(),
                    "Заявка обработана", triggeredBy, userName);

            log.info("✅ Заявка {} обработана (статус PROCESSED). Инициатор: {}",
                    request.getId(), triggeredBy);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при обработке заявки {}: {}", request.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Завершает заявку (переводит в статус COMPLETED)
     */
    @Transactional
    public boolean completeRequest(Request request, String triggeredBy, String userName) {
        if (!canCompleteRequest(request)) {
            log.warn("Заявка {} не может быть завершена", request.getId());
            return false;
        }

        try {
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            requestRepository.save(request);

            addHistory(request, RequestStatus.COMPLETED.name(),
                    "Заявка завершена", triggeredBy, userName);

            log.info("✅ Заявка {} завершена (статус COMPLETED). Инициатор: {}",
                    request.getId(), triggeredBy);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при завершении заявки {}: {}", request.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Ручная обработка заявки пользователем
     */
    @Transactional
    public boolean manualProcessRequest(Long requestId, String username, String fullName) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            log.warn("Заявка {} не найдена", requestId);
            return false;
        }

        if (!canProcessRequest(request)) {
            log.warn("Заявка {} не готова к обработке", requestId);
            return false;
        }

        return processRequest(request, "user:" + username, fullName);
    }

    /**
     * Ручное завершение заявки пользователем
     */
    @Transactional
    public boolean manualCompleteRequest(Long requestId, String username, String fullName) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            log.warn("Заявка {} не найдена", requestId);
            return false;
        }

        if (!canCompleteRequest(request)) {
            log.warn("Заявка {} не может быть завершена", requestId);
            return false;
        }

        return completeRequest(request, "user:" + username, fullName);
    }

    /**
     * Поиск заявок, готовых к обработке
     */
    public List<Request> findRequestsReadyForProcessing() {
        List<Request> inProgressRequests = requestRepository.findByStatus(RequestStatus.IN_PROGRESS);

        return inProgressRequests.stream()
                .filter(this::canProcessRequest)
                .toList();
    }

    /**
     * Поиск заявок, готовых к завершению
     */
    public List<Request> findRequestsReadyForCompletion() {
        List<Request> processedRequests = requestRepository.findByStatus(RequestStatus.PROCESSED);

        return processedRequests.stream()
                .filter(this::canCompleteRequest)
                .toList();
    }

    /**
     * Автоматическая обработка всех готовых заявок
     */
    @Transactional
    public int autoProcessAllReadyRequests() {
        if (!settings.isAutoProcessEnabled()) {
            log.debug("Автоматическая обработка заявок отключена");
            return 0;
        }

        List<Request> readyRequests = findRequestsReadyForProcessing();
        int processed = 0;

        for (Request request : readyRequests) {
            if (isDelayPassed(request.getCreatedAt(), settings.getAutoProcessDelayHours())) {
                if (processRequest(request, "system_auto_process", "Система")) {
                    processed++;
                }
            }
        }

        if (processed > 0) {
            log.info("Автоматически обработано {} заявок", processed);
        }

        return processed;
    }

    /**
     * Автоматическое завершение всех готовых заявок
     */
    @Transactional
    public int autoCompleteAllReadyRequests() {
        if (!settings.isAutoCompleteEnabled()) {
            log.debug("Автоматическое завершение заявок отключено");
            return 0;
        }

        List<Request> readyRequests = findRequestsReadyForCompletion();
        int completed = 0;

        for (Request request : readyRequests) {
            if (request.getCompletedAt() != null &&
                    isDelayPassed(request.getCompletedAt(), settings.getAutoCompleteDelayHours())) {
                if (completeRequest(request, "system_auto_complete", "Система")) {
                    completed++;
                }
            }
        }

        if (completed > 0) {
            log.info("Автоматически завершено {} заявок", completed);
        }

        return completed;
    }

    /**
     * Проверка, прошла ли задержка с указанного времени
     */
    private boolean isDelayPassed(LocalDateTime fromTime, double delayHours) {
        if (fromTime == null) return false;
        long delayMillis = (long) (delayHours * 3600 * 1000);
        return fromTime.plusNanos(delayMillis * 1_000_000).isBefore(LocalDateTime.now());
    }

    /**
     * Добавление записи в историю
     */
    private void addHistory(Request request, String status, String action, String changedBy, String userName) {
        org.example.model.RequestHistory history = new org.example.model.RequestHistory();
        history.setRequest(request);
        history.setStatus(status);
        history.setAction(action);
        history.setChangedBy(changedBy);
        history.setUserName(userName);
        requestHistoryRepository.save(history);
    }
}