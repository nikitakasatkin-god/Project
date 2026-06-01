package org.example.service;

import org.example.model.Request;
import org.example.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ЗАГЛУШКА для интеграции с внешними системами:
 * - 1С (синхронизация данных о контрагентах, номенклатуре)
 * - Система диспетчеризации рейсов (обмен статусами)
 */
@Service
public class ExternalSystemStub {

    private static final Logger log = LoggerFactory.getLogger(ExternalSystemStub.class);

    // Хранение "отправленных" данных (заглушка БД)
    private final ConcurrentHashMap<Long, Object> syncQueue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> dispatchQueue = new ConcurrentHashMap<>();

    /**
     * Заглушка синхронизации с 1С
     * В реальной системе здесь был бы HTTP запрос к API 1С или обмен через RabbitMQ/Kafka
     */
    public void syncWith1C(Request request) {
        log.info("=== ЗАГЛУШКА: Синхронизация с 1С ===");
        log.info("Отправляем заявку №{} в 1С", request.getId());
        log.info("Данные: объем={}т, маршрут={}->{}",
                request.getVolume(), request.getPickupPoint(), request.getDeliveryPoint());

        // Имитация сохранения в очередь синхронизации
        syncQueue.put(request.getId(), Map.of(
                "requestId", request.getId(),
                "syncTime", LocalDateTime.now(),
                "status", "PENDING",
                "system", "1C"
        ));

        // Имитация ответа от 1С (получили бы контрагента, договор, цену и т.д.)
        simulate1CResponse(request);

        log.info("=== Синхронизация с 1С завершена (ЗАГЛУШКА) ===");
    }

    private void simulate1CResponse(Request request) {
        // Заглушка: имитируем получение данных из 1С
        // В реальности это было бы асинхронно через webhook или долгий poll

        // Данные, которые обычно приходят из 1С:
        // - Номер договора с контрагентом
        // - Ставка за перевозку
        // - Код номенклатуры
        // - Коды складов отгрузки/получения

        log.info("Имитация ответа от 1С для заявки №{}:", request.getId());
        log.info("  - Договор: ДОГ-{}/2024", 1000 + request.getId());
        log.info("  - Ставка: {:.2f} руб/т", 1500 + (request.getVolume() * 10));
        log.info("  - Код номенклатуры: N-{}", 50000 + request.getId());

        syncQueue.put(request.getId(), Map.of(
                "requestId", request.getId(),
                "syncTime", LocalDateTime.now(),
                "status", "COMPLETED",
                "contractNumber", "ДОГ-" + (1000 + request.getId()) + "/2024",
                "rate", 1500 + (request.getVolume() * 10)
        ));
    }

    /**
     * Заглушка отправки рейса в систему диспетчеризации
     */
    public void sendToDispatchSystem(Trip trip) {
        log.info("=== ЗАГЛУШКА: Отправка в систему диспетчеризации ===");
        log.info("Рейс №{} (заявка №{}) отправлен в систему диспетчеризации",
                trip.getId(), trip.getRequest().getId());
        log.info("Данные рейса:");
        log.info("  - Перевозчик: {}", trip.getCarrier().getName());
        log.info("  - ТС: {} ({})", trip.getVehiclePlate(), trip.getVehicleBrand());
        log.info("  - Водитель: {}", trip.getDriverName());
        log.info("  - Дата рейса: {}", trip.getTripDate());
        log.info("  - Объем: {} т", trip.getVolume());
        log.info("  - Маршрут: {} -> {}",
                trip.getRequest().getPickupPoint(),
                trip.getRequest().getDeliveryPoint());

        dispatchQueue.put(trip.getId(), Map.of(
                "tripId", trip.getId(),
                "dispatchTime", LocalDateTime.now(),
                "status", "SENT_TO_DISPATCH"
        ));

        // Имитация получения обновлений статуса от системы диспетчеризации
        simulateDispatchStatusUpdates(trip);

        log.info("=== Отправка в систему диспетчеризации завершена (ЗАГЛУШКА) ===");
    }

    private void simulateDispatchStatusUpdates(Trip trip) {
        // Заглушка: имитация получения статусов от системы диспетчеризации
        // В реальности это было бы через WebSocket, Kafka или REST callback

        // Здесь мы просто симулируем, что система диспетчеризации вернет статусы
        // В реальном приложении эти статусы приходили бы асинхронно
        log.info("Имитация получения статусов от системы диспетчеризации для рейса №{}:", trip.getId());
        log.info("  - Статус: IN_TRANSIT (в пути)");
        log.info("  - Статус: ARRIVED_UNLOADING (прибыл на выгрузку)");
        log.info("  - Статус: UNLOADED (выгружен)");
        log.info("  - Статус: PROCESSED (обработан)");

        dispatchQueue.put(trip.getId(), Map.of(
                "tripId", trip.getId(),
                "lastUpdateTime", LocalDateTime.now(),
                "lastStatus", "PROCESSED"
        ));
    }

    /**
     * Заглушка получения статуса синхронизации с 1С
     */
    public Object getSyncStatus(Long requestId) {
        return syncQueue.getOrDefault(requestId, Map.of("status", "NOT_FOUND"));
    }

    /**
     * Заглушка получения статуса рейса в системе диспетчеризации
     */
    public Object getDispatchStatus(Long tripId) {
        return dispatchQueue.getOrDefault(tripId, Map.of("status", "NOT_FOUND"));
    }

    /**
     * Заглушка ручного обновления статуса из системы диспетчеризации
     * (для тестирования можно вызывать этот метод вручную)
     */
    public void manualUpdateDispatchStatus(Long tripId, String newStatus) {
        log.info("=== РУЧНОЕ ОБНОВЛЕНИЕ СТАТУСА (ЗАГЛУШКА) ===");
        log.info("Рейс №{} получил статус '{}' от системы диспетчеризации", tripId, newStatus);

        dispatchQueue.put(tripId, Map.of(
                "tripId", tripId,
                "updateTime", LocalDateTime.now(),
                "status", newStatus,
                "source", "MANUAL_UPDATE"
        ));
    }
}