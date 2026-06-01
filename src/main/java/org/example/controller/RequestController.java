package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ExternalSystemStub externalSystemStub;

    public RequestController(RequestRepository requestRepository,
                             UserRepository userRepository,
                             ExternalSystemStub externalSystemStub) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.externalSystemStub = externalSystemStub;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public List<Request> getRequests(@RequestParam(required = false) String type) {
        User currentUser = getCurrentUser();
        List<Request> requests;

        if (currentUser.getRole() == Role.ADMIN) {
            requests = requestRepository.findAll();
        } else if (currentUser.getRole() == Role.LOGIST) {
            requests = requestRepository.findByOwner(currentUser);
        } else {
            requests = requestRepository.findByDivision(currentUser.getDivision());
        }

        if (type != null && !type.isEmpty()) {
            requests = requests.stream()
                    .filter(r -> r.getProductType() != null && r.getProductType().name().equals(type))
                    .toList();
        }

        return requests;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Request> getRequest(@PathVariable Long id) {
        return requestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/pickup-points")
    public List<Map<String, String>> getPickupPoints() {
        List<Map<String, String>> points = new java.util.ArrayList<>();

        // В реальном приложении данные берутся из базы
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

        // В реальном приложении данные берутся из базы
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

            // Проверка обязательных полей
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

            // Определяем тип продукции
            String productType = data.containsKey("productType") ?
                    data.get("productType").toString() : "BRANDED";
            request.setProductType(ProductType.valueOf(productType));

            request.setStatus(RequestStatus.NEW);

            Request saved = requestRepository.save(request);

            // Заглушка: отправка в 1С
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

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Доступ запрещен");
        }

        if (request.getStatus() == RequestStatus.NEW) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            requestRepository.save(request);
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

        // Только создатель заявки (логист) или админ могут удалить новую заявку
        if (request.getStatus() == RequestStatus.NEW &&
                (request.getOwner().getId().equals(currentUser.getId()) ||
                        currentUser.getRole() == Role.ADMIN)) {
            requestRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно");
    }
}