package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.example.service.RequestAutoProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.model.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final ExternalSystemStub externalSystemStub;
    private final RequestAutoProcessingService processingService;

    public RequestController(RequestRepository requestRepository,
                             RequestHistoryRepository requestHistoryRepository,
                             UserRepository userRepository,
                             DivisionRepository divisionRepository,
                             ExternalSystemStub externalSystemStub,
                             RequestAutoProcessingService processingService) {
        this.requestRepository = requestRepository;
        this.requestHistoryRepository = requestHistoryRepository;
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.externalSystemStub = externalSystemStub;
        this.processingService = processingService;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private void addHistory(Request request, String status, String action, String changedBy, String userName) {
        RequestHistory history = new RequestHistory();
        history.setRequest(request);
        history.setStatus(status);
        history.setAction(action);
        history.setChangedBy(changedBy);
        history.setUserName(userName);
        requestHistoryRepository.save(history);
    }

    @GetMapping
    public List<Request> getRequests(@RequestParam(required = false) String type) {
        User currentUser = getCurrentUser();
        List<Request> requests;

        if (currentUser.getRole() == Role.ADMIN) {
            requests = requestRepository.findAll();
        } else if (currentUser.getRole() == Role.LOGIST) {
            requests = requestRepository.findByOwner(currentUser);
        } else if (currentUser.getRole() == Role.DISPATCHER) {
            requests = requestRepository.findByDivision(currentUser.getDivision());
        } else {
            requests = List.of();
        }

        if (type != null && !type.isEmpty()) {
            requests = requests.stream()
                    .filter(r -> r.getProductType() != null && r.getProductType().name().equals(type))
                    .toList();
        }

        return requests;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequest(@PathVariable Long id) {
        return requestRepository.findById(id)
                .map(request -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", request.getId());
                    response.put("owner", request.getOwner());
                    response.put("division", request.getDivision());
                    response.put("productType", request.getProductType());
                    response.put("volume", request.getVolume());
                    response.put("pickupPoint", request.getPickupPoint());
                    response.put("deliveryPoint", request.getDeliveryPoint());
                    response.put("pickupStartDate", request.getPickupStartDate());
                    response.put("pickupEndDate", request.getPickupEndDate());
                    response.put("pickupStartTime", request.getPickupStartTime());
                    response.put("pickupEndTime", request.getPickupEndTime());
                    response.put("status", request.getStatus());
                    response.put("createdAt", request.getCreatedAt());
                    response.put("completedAt", request.getCompletedAt());
                    response.put("trips", request.getTrips());
                    response.put("history", requestHistoryRepository.findByRequestOrderByChangedAtAsc(request));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RequestHistory>> getRequestHistory(@PathVariable Long id) {
        return requestRepository.findById(id)
                .map(request -> ResponseEntity.ok(requestHistoryRepository.findByRequestOrderByChangedAtAsc(request)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/pickup-points")
    public List<Map<String, String>> getPickupPoints() {
        List<Map<String, String>> points = new java.util.ArrayList<>();

        String[][] plants = {
                {"Завод №1", "🏭 Завод: Завод №1"},
                {"Завод №2", "🏭 Завод: Завод №2"},
                {"Завод №3", "🏭 Завод: Завод №3"},
                {"Завод №4", "🏭 Завод: Завод №4"},
                {"Завод №5", "🏭 Завод: Завод №5"}
        };

        for (String[] plant : plants) {
            Map<String, String> p = new HashMap<>();
            p.put("value", plant[0]);
            p.put("label", plant[1]);
            points.add(p);
        }

        return points;
    }

    @GetMapping("/reference/delivery-points")
    public List<Map<String, String>> getDeliveryPoints() {
        List<Map<String, String>> points = new java.util.ArrayList<>();

        String[][] warehouses = {
                {"Склад №1", "📦 Склад: Склад №1"},
                {"Склад №2", "📦 Склад: Склад №2"},
                {"Склад №3", "📦 Склад: Склад №3"},
                {"Склад №4", "📦 Склад: Склад №4"},
                {"Склад №5", "📦 Склад: Склад №5"}
        };

        for (String[] warehouse : warehouses) {
            Map<String, String> w = new HashMap<>();
            w.put("value", warehouse[0]);
            w.put("label", warehouse[1]);
            points.add(w);
        }

        return points;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> data) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Пользователь не авторизован"));
            }

            if (currentUser.getRole() != Role.LOGIST && currentUser.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен. Только логист или администратор могут создавать заявки"));
            }

            List<String> missingFields = new java.util.ArrayList<>();
            if (data.get("volume") == null) missingFields.add("Объем");
            if (data.get("pickupPoint") == null) missingFields.add("Пункт погрузки");
            if (data.get("deliveryPoint") == null) missingFields.add("Пункт разгрузки");
            if (data.get("pickupStartDate") == null) missingFields.add("Дата начала погрузки");
            if (data.get("pickupEndDate") == null) missingFields.add("Дата окончания погрузки");
            if (data.get("pickupStartTime") == null) missingFields.add("Время начала погрузки");
            if (data.get("pickupEndTime") == null) missingFields.add("Время окончания погрузки");

            if (!missingFields.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Не все обязательные поля заполнены",
                        "missingFields", missingFields
                ));
            }

            Request request = new Request();
            request.setOwner(currentUser);
            request.setDivision(currentUser.getDivision());
            request.setVolume(Double.parseDouble(data.get("volume").toString()));
            request.setPickupPoint(data.get("pickupPoint").toString());
            request.setDeliveryPoint(data.get("deliveryPoint").toString());
            request.setPickupStartDate(LocalDate.parse(data.get("pickupStartDate").toString()));
            request.setPickupEndDate(LocalDate.parse(data.get("pickupEndDate").toString()));
            request.setPickupStartTime(LocalTime.parse(data.get("pickupStartTime").toString()));
            request.setPickupEndTime(LocalTime.parse(data.get("pickupEndTime").toString()));

            String productType = data.containsKey("productType") ?
                    data.get("productType").toString() : "BRANDED";
            request.setProductType(ProductType.valueOf(productType));

            request.setStatus(RequestStatus.NEW);

            Request saved = requestRepository.save(request);

            addHistory(saved, "NEW", "Создание заявки", "user:" + currentUser.getUsername(), currentUser.getFullName());

            externalSystemStub.syncWith1C(saved);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка успешно создана",
                    "requestId", saved.getId()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            Request request = requestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }

            User currentUser = getCurrentUser();

            boolean isOwner = currentUser.getId().equals(request.getOwner().getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;

            if ((!isOwner && !isAdmin) || request.getStatus() != RequestStatus.NEW) {
                return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен. Редактирование возможно только для заявок в статусе 'Новая' и только владельцем или администратором"));
            }

            StringBuilder changes = new StringBuilder();

            if (data.containsKey("volume")) {
                double oldVolume = request.getVolume();
                double newVolume = Double.parseDouble(data.get("volume").toString());
                if (oldVolume != newVolume) {
                    changes.append("Объем: ").append(oldVolume).append(" → ").append(newVolume).append("; ");
                }
                request.setVolume(newVolume);
            }
            if (data.containsKey("pickupPoint")) {
                String oldValue = request.getPickupPoint();
                String newValue = data.get("pickupPoint").toString();
                if (!oldValue.equals(newValue)) {
                    changes.append("Пункт погрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setPickupPoint(newValue);
            }
            if (data.containsKey("deliveryPoint")) {
                String oldValue = request.getDeliveryPoint();
                String newValue = data.get("deliveryPoint").toString();
                if (!oldValue.equals(newValue)) {
                    changes.append("Пункт разгрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setDeliveryPoint(newValue);
            }
            if (data.containsKey("pickupStartDate")) {
                LocalDate oldValue = request.getPickupStartDate();
                LocalDate newValue = LocalDate.parse(data.get("pickupStartDate").toString());
                if (!oldValue.equals(newValue)) {
                    changes.append("Дата начала погрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setPickupStartDate(newValue);
            }
            if (data.containsKey("pickupEndDate")) {
                LocalDate oldValue = request.getPickupEndDate();
                LocalDate newValue = LocalDate.parse(data.get("pickupEndDate").toString());
                if (!oldValue.equals(newValue)) {
                    changes.append("Дата окончания погрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setPickupEndDate(newValue);
            }
            if (data.containsKey("pickupStartTime")) {
                LocalTime oldValue = request.getPickupStartTime();
                LocalTime newValue = LocalTime.parse(data.get("pickupStartTime").toString());
                if (!oldValue.equals(newValue)) {
                    changes.append("Время начала погрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setPickupStartTime(newValue);
            }
            if (data.containsKey("pickupEndTime")) {
                LocalTime oldValue = request.getPickupEndTime();
                LocalTime newValue = LocalTime.parse(data.get("pickupEndTime").toString());
                if (!oldValue.equals(newValue)) {
                    changes.append("Время окончания погрузки: ").append(oldValue).append(" → ").append(newValue).append("; ");
                }
                request.setPickupEndTime(newValue);
            }

            Request saved = requestRepository.save(request);

            if (changes.length() > 0) {
                addHistory(saved, saved.getStatus().name(), "Редактирование: " + changes.toString(),
                        "user:" + currentUser.getUsername(), currentUser.getFullName());
            }

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Доступ запрещен. Только диспетчер или администратор могут обработать заявку");
        }

        if (request.getStatus() == RequestStatus.NEW) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            requestRepository.save(request);

            addHistory(request, RequestStatus.IN_PROGRESS.name(), "Заявка принята в работу",
                    "user:" + currentUser.getUsername(), currentUser.getFullName());

            return ResponseEntity.ok(request);
        }

        return ResponseEntity.badRequest().body("Заявка не в статусе 'Новая'");
    }

    /**
     * Отклонение заявки (только для статуса NEW)
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        // ДИАГНОСТИКА НАЧАЛО
        System.out.println("========== ОТКЛОНЕНИЕ ЗАЯВКИ НАЧАЛО ==========");
        System.out.println("ID заявки: " + id);
        System.out.println("Полученные данные: " + data);
        System.out.println("Текущее время: " + LocalDateTime.now());

        try {
            // Шаг 1: Поиск заявки
            System.out.println("Шаг 1: Поиск заявки в БД...");
            Request request = requestRepository.findById(id).orElse(null);
            if (request == null) {
                System.out.println("ОШИБКА ШАГ 1: Заявка не найдена с id=" + id);
                return ResponseEntity.status(404).body(Map.of("error", "Заявка не найдена"));
            }
            System.out.println("Шаг 1: Заявка найдена, статус=" + request.getStatus());

            // Шаг 2: Получение текущего пользователя
            System.out.println("Шаг 2: Получение текущего пользователя...");
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                System.out.println("ОШИБКА ШАГ 2: Текущий пользователь не найден");
                return ResponseEntity.status(401).body(Map.of("error", "Пользователь не авторизован"));
            }
            System.out.println("Шаг 2: Пользователь найден: username=" + currentUser.getUsername() +
                    ", role=" + currentUser.getRole() +
                    ", id=" + currentUser.getId());

            // Шаг 3: Проверка прав доступа
            System.out.println("Шаг 3: Проверка прав доступа...");
            boolean isDispatcher = currentUser.getRole() == Role.DISPATCHER;
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            System.out.println("  - isDispatcher: " + isDispatcher);
            System.out.println("  - isAdmin: " + isAdmin);

            if (!isDispatcher && !isAdmin) {
                System.out.println("ОШИБКА ШАГ 3: Недостаточно прав. Роль пользователя: " + currentUser.getRole());
                return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен. Только диспетчер или администратор могут отклонить заявку"));
            }
            System.out.println("Шаг 3: Права доступа подтверждены");

            // Шаг 4: Проверка статуса заявки
            System.out.println("Шаг 4: Проверка статуса заявки...");
            System.out.println("  - Текущий статус: " + request.getStatus());
            System.out.println("  - Ожидаемый статус: NEW");

            if (request.getStatus() != RequestStatus.NEW) {
                System.out.println("ОШИБКА ШАГ 4: Неверный статус заявки. Ожидался NEW, получен " + request.getStatus());
                return ResponseEntity.badRequest().body(Map.of("error", "Отклонить можно только заявку в статусе 'Новая'. Текущий статус: " + request.getStatus().getDisplayName()));
            }
            System.out.println("Шаг 4: Статус заявки корректен");

            // Шаг 5: Получение и проверка причины отклонения
            System.out.println("Шаг 5: Получение причины отклонения...");
            String reason = data.get("reason") != null ? data.get("reason").toString() : "";
            System.out.println("  - Причина: '" + reason + "'");
            System.out.println("  - Длина причины: " + reason.length() + " символов");
            System.out.println("  - Пустая после trim: " + reason.trim().isEmpty());

            if (reason.trim().isEmpty()) {
                System.out.println("ОШИБКА ШАГ 5: Причина отклонения не указана или пустая");
                return ResponseEntity.badRequest().body(Map.of("error", "Укажите причину отклонения"));
            }
            System.out.println("Шаг 5: Причина указана корректно");

            // Шаг 6: Изменение статуса заявки
            System.out.println("Шаг 6: Изменение статуса заявки...");
            System.out.println("  - Старый статус: " + request.getStatus());
            request.setStatus(RequestStatus.REJECTED);
            System.out.println("  - Новый статус: " + request.getStatus());

            System.out.println("Шаг 7: Сохранение заявки в БД...");
            Request savedRequest = requestRepository.save(request);
            System.out.println("Шаг 7: Заявка сохранена, ID=" + savedRequest.getId() + ", статус=" + savedRequest.getStatus());

            // Шаг 8: Добавление записи в историю
            System.out.println("Шаг 8: Добавление записи в историю...");
            String actionText = "Заявка отклонена. Причина: " + reason;
            String changedBy = "user:" + currentUser.getUsername();
            String userName = currentUser.getFullName();
            System.out.println("  - action: " + actionText);
            System.out.println("  - changedBy: " + changedBy);
            System.out.println("  - userName: " + userName);

            addHistory(request, RequestStatus.REJECTED.name(), actionText, changedBy, userName);
            System.out.println("Шаг 8: История добавлена");

            // УСПЕХ
            System.out.println("========== ОТКЛОНЕНИЕ ЗАЯВКИ УСПЕШНО ЗАВЕРШЕНО ==========");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка отклонена",
                    "newStatus", RequestStatus.REJECTED.name()
            ));

        } catch (Exception e) {
            System.out.println("========== ОШИБКА ПРИ ОТКЛОНЕНИИ ЗАЯВКИ ==========");
            System.out.println("Тип исключения: " + e.getClass().getName());
            System.out.println("Сообщение: " + e.getMessage());
            System.out.println("Полный stack trace:");
            e.printStackTrace();
            System.out.println("========== КОНЕЦ ОШИБКИ ==========");
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Внутренняя ошибка сервера: " + e.getMessage(),
                    "exceptionType", e.getClass().getName()
            ));
        }
    }

    /**
     * Проверка, можно ли отклонить заявку
     */
    @GetMapping("/{id}/can-reject")
    public ResponseEntity<?> canRejectRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("canReject", request.getStatus() == RequestStatus.NEW);
        response.put("currentStatus", request.getStatus().name());
        return ResponseEntity.ok(response);
    }

    /**
     * Ручная обработка заявки (кнопка "Завершить обработку")
     */
    @PostMapping("/{id}/complete-processing")
    public ResponseEntity<?> completeProcessing(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен. Только диспетчер или администратор могут завершить обработку заявки"));
        }

        boolean success = processingService.manualProcessRequest(id, currentUser.getUsername(), currentUser.getFullName());

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка обработана",
                    "newStatus", RequestStatus.PROCESSED.name()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Невозможно обработать заявку. Проверьте, что объем полностью распределен и все рейсы имеют статус 'Обработан'"
            ));
        }
    }

    /**
     * Проверка, можно ли обработать заявку
     */
    @GetMapping("/{id}/can-process")
    public ResponseEntity<?> canProcessRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("canProcess", processingService.canProcessRequest(request));
        response.put("currentStatus", request.getStatus().name());

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            response.put("reason", "Заявка не в статусе 'В работе'");
            return ResponseEntity.ok(response);
        }

        double totalVolume = request.getVolume();
        double assignedVolume = request.getTrips().stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .mapToDouble(Trip::getVolume)
                .sum();

        if (Math.abs(totalVolume - assignedVolume) > 0.01) {
            response.put("reason", String.format("Объем распределен не полностью (%.1f из %.1f т)", assignedVolume, totalVolume));
            response.put("assignedVolume", assignedVolume);
            response.put("totalVolume", totalVolume);
            return ResponseEntity.ok(response);
        }

        boolean allTripsProcessed = request.getTrips().stream()
                .filter(t -> t.getStatus() != TripStatus.DELETED && t.getStatus() != TripStatus.CANCELLED)
                .allMatch(t -> t.getStatus() == TripStatus.PROCESSED);

        if (!allTripsProcessed) {
            response.put("reason", "Не все рейсы имеют статус 'Обработан'");
            return ResponseEntity.ok(response);
        }

        response.put("assignedVolume", assignedVolume);
        response.put("totalVolume", totalVolume);
        return ResponseEntity.ok(response);
    }

    /**
     * Ручное завершение заявки
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен"));
        }

        if (request.getStatus() != RequestStatus.PROCESSED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Заявка не в статусе 'Обработана'"));
        }

        boolean success = processingService.manualCompleteRequest(id, currentUser.getUsername(), currentUser.getFullName());

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка завершена",
                    "newStatus", RequestStatus.COMPLETED.name()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Невозможно завершить заявку"
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (request.getStatus() == RequestStatus.NEW &&
                (request.getOwner().getId().equals(currentUser.getId()) ||
                        currentUser.getRole() == Role.ADMIN)) {

            addHistory(request, "DELETED", "Заявка удалена",
                    "user:" + currentUser.getUsername(), currentUser.getFullName());

            requestRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно");
    }
}