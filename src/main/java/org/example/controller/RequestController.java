package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    public RequestController(RequestRepository requestRepository,
                             RequestHistoryRepository requestHistoryRepository,
                             UserRepository userRepository,
                             DivisionRepository divisionRepository,
                             ExternalSystemStub externalSystemStub) {
        this.requestRepository = requestRepository;
        this.requestHistoryRepository = requestHistoryRepository;
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.externalSystemStub = externalSystemStub;
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