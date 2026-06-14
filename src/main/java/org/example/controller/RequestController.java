package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.example.service.RequestAutoProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepository;
    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final ProductRepository productRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final ExternalSystemStub externalSystemStub;
    private final RequestAutoProcessingService processingService;

    public RequestController(RequestRepository requestRepository,
                             RequestHistoryRepository requestHistoryRepository,
                             UserRepository userRepository,
                             DivisionRepository divisionRepository,
                             ProductRepository productRepository,
                             PlantRepository plantRepository,
                             WarehouseRepository warehouseRepository,
                             ExternalSystemStub externalSystemStub,
                             RequestAutoProcessingService processingService) {
        this.requestRepository = requestRepository;
        this.requestHistoryRepository = requestHistoryRepository;
        this.userRepository = userRepository;
        this.divisionRepository = divisionRepository;
        this.productRepository = productRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
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

    @GetMapping("/reference/products")
    public List<Product> getProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getActive() != null && p.getActive())
                .collect(Collectors.toList());
    }

    // ========== ЭНДПОИНТЫ ДЛЯ ЗАВОДОВ И СКЛАДОВ ==========
    @GetMapping("/reference/pickup-points")
    public List<Plant> getPickupPoints() {
        return plantRepository.findAll();
    }

    @GetMapping("/reference/delivery-points")
    public List<Warehouse> getDeliveryPoints() {
        return warehouseRepository.findAll();
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
                    .collect(Collectors.toList());
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
                    response.put("product", request.getProduct());
                    response.put("volume", request.getVolume());
                    // Отдаем объекты с id и name
                    response.put("pickupPlant", request.getPickupPlant());
                    response.put("deliveryWarehouse", request.getDeliveryWarehouse());
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
            if (data.get("productId") == null) missingFields.add("Продукт");
            if (data.get("pickupPlantId") == null && data.get("pickupPoint") == null) missingFields.add("Пункт погрузки");
            if (data.get("deliveryWarehouseId") == null && data.get("deliveryPoint") == null) missingFields.add("Пункт разгрузки");
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

            Long productId = Long.parseLong(data.get("productId").toString());
            Product product = productRepository.findById(productId).orElse(null);
            request.setProduct(product);

            // ========== ЗАГРУЗКА ЗАВОДА И СКЛАДА ПО ID ==========
            if (data.containsKey("pickupPlantId")) {
                Long plantId = Long.parseLong(data.get("pickupPlantId").toString());
                Plant plant = plantRepository.findById(plantId).orElse(null);
                request.setPickupPlant(plant);
            } else if (data.containsKey("pickupPoint")) {
                // Для обратной совместимости: ищем завод по имени
                String plantName = data.get("pickupPoint").toString();
                Plant plant = plantRepository.findByName(plantName).orElse(null);
                request.setPickupPlant(plant);
            }

            if (data.containsKey("deliveryWarehouseId")) {
                Long warehouseId = Long.parseLong(data.get("deliveryWarehouseId").toString());
                Warehouse warehouse = warehouseRepository.findById(warehouseId).orElse(null);
                request.setDeliveryWarehouse(warehouse);
            } else if (data.containsKey("deliveryPoint")) {
                // Для обратной совместимости: ищем склад по имени
                String warehouseName = data.get("deliveryPoint").toString();
                Warehouse warehouse = warehouseRepository.findByName(warehouseName).orElse(null);
                request.setDeliveryWarehouse(warehouse);
            }
            // =================================================

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

            if (data.containsKey("productId")) {
                Long oldProductId = request.getProduct() != null ? request.getProduct().getId() : null;
                Long newProductId = Long.parseLong(data.get("productId").toString());
                if (oldProductId == null || !oldProductId.equals(newProductId)) {
                    Product newProduct = productRepository.findById(newProductId).orElse(null);
                    if (newProduct != null) {
                        String oldProductName = request.getProduct() != null ? request.getProduct().getName() : "не указан";
                        changes.append("Продукт: ").append(oldProductName).append(" → ").append(newProduct.getName()).append("; ");
                        request.setProduct(newProduct);
                    }
                }
            }

            // ========== ОБНОВЛЕНИЕ ЗАВОДА ==========
            if (data.containsKey("pickupPlantId")) {
                Long newPlantId = Long.parseLong(data.get("pickupPlantId").toString());
                Plant newPlant = plantRepository.findById(newPlantId).orElse(null);
                if (newPlant != null) {
                    String oldPlantName = request.getPickupPlant() != null ? request.getPickupPlant().getName() : "не указан";
                    changes.append("Пункт погрузки: ").append(oldPlantName).append(" → ").append(newPlant.getName()).append("; ");
                    request.setPickupPlant(newPlant);
                }
            } else if (data.containsKey("pickupPoint")) {
                String newPlantName = data.get("pickupPoint").toString();
                Plant newPlant = plantRepository.findByName(newPlantName).orElse(null);
                if (newPlant != null) {
                    String oldPlantName = request.getPickupPlant() != null ? request.getPickupPlant().getName() : "не указан";
                    changes.append("Пункт погрузки: ").append(oldPlantName).append(" → ").append(newPlant.getName()).append("; ");
                    request.setPickupPlant(newPlant);
                }
            }

            // ========== ОБНОВЛЕНИЕ СКЛАДА ==========
            if (data.containsKey("deliveryWarehouseId")) {
                Long newWarehouseId = Long.parseLong(data.get("deliveryWarehouseId").toString());
                Warehouse newWarehouse = warehouseRepository.findById(newWarehouseId).orElse(null);
                if (newWarehouse != null) {
                    String oldWarehouseName = request.getDeliveryWarehouse() != null ? request.getDeliveryWarehouse().getName() : "не указан";
                    changes.append("Пункт разгрузки: ").append(oldWarehouseName).append(" → ").append(newWarehouse.getName()).append("; ");
                    request.setDeliveryWarehouse(newWarehouse);
                }
            } else if (data.containsKey("deliveryPoint")) {
                String newWarehouseName = data.get("deliveryPoint").toString();
                Warehouse newWarehouse = warehouseRepository.findByName(newWarehouseName).orElse(null);
                if (newWarehouse != null) {
                    String oldWarehouseName = request.getDeliveryWarehouse() != null ? request.getDeliveryWarehouse().getName() : "не указан";
                    changes.append("Пункт разгрузки: ").append(oldWarehouseName).append(" → ").append(newWarehouse.getName()).append("; ");
                    request.setDeliveryWarehouse(newWarehouse);
                }
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

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.DISPATCHER && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Доступ запрещен. Только диспетчер или администратор могут отклонить заявку"));
        }

        if (request.getStatus() != RequestStatus.NEW) {
            return ResponseEntity.badRequest().body(Map.of("error", "Отклонить можно только заявку в статусе 'Новая'"));
        }

        String reason = data.get("reason") != null ? data.get("reason").toString() : "";
        if (reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Укажите причину отклонения"));
        }

        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);

        addHistory(request, RequestStatus.REJECTED.name(),
                "Заявка отклонена. Причина: " + reason,
                "user:" + currentUser.getUsername(),
                currentUser.getFullName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Заявка отклонена",
                "newStatus", RequestStatus.REJECTED.name()
        ));
    }

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