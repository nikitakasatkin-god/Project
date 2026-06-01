package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.example.service.ExternalSystemStub;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final CarrierRepository carrierRepository;
    private final DivisionRepository divisionRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final ExternalSystemStub externalSystemStub;

    public RequestController(RequestRepository requestRepository,
                             UserRepository userRepository,
                             TripRepository tripRepository,
                             CarrierRepository carrierRepository,
                             DivisionRepository divisionRepository,
                             PlantRepository plantRepository,
                             WarehouseRepository warehouseRepository,
                             ExternalSystemStub externalSystemStub) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.carrierRepository = carrierRepository;
        this.divisionRepository = divisionRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.externalSystemStub = externalSystemStub;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public List<Request> getRequests(@RequestParam(required = false) String type,
                                     @RequestParam(required = false) String status) {
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
                    .filter(r -> r.getProductType().name().equals(type))
                    .toList();
        }

        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            requests = requests.stream()
                    .filter(r -> r.getStatus().name().equals(status))
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

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> data) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != Role.LOGIST && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Доступ запрещен");
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
        request.setProductType(ProductType.valueOf(data.get("productType").toString()));
        request.setStatus(RequestStatus.NEW);

        externalSystemStub.syncWith1C(request);

        Request saved = requestRepository.save(request);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();

        if (request.getStatus() == RequestStatus.NEW) {
            if (currentUser.getRole() == Role.LOGIST &&
                    request.getOwner().getDivision().getId().equals(currentUser.getDivision().getId())) {

                if (data.containsKey("volume")) request.setVolume(Double.parseDouble(data.get("volume").toString()));
                if (data.containsKey("pickupPoint")) request.setPickupPoint(data.get("pickupPoint").toString());
                if (data.containsKey("deliveryPoint")) request.setDeliveryPoint(data.get("deliveryPoint").toString());

                if (data.containsKey("pickupStartDate")) {
                    LocalDate newDate = LocalDate.parse(data.get("pickupStartDate").toString());
                    if (!newDate.isBefore(LocalDate.now())) {
                        request.setPickupStartDate(newDate);
                    }
                }

                requestRepository.save(request);
                return ResponseEntity.ok(request);
            }
        }

        return ResponseEntity.status(403).body("Редактирование недоступно");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();

        if (request.getStatus() == RequestStatus.NEW &&
                request.getOwner().getId().equals(currentUser.getId())) {
            requestRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(403).body("Удаление недоступно");
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processRequest(@PathVariable Long id) {
        Request request = requestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();

        if (request.getStatus() == RequestStatus.NEW &&
                (currentUser.getRole() == Role.DISPATCHER || currentUser.getRole() == Role.ADMIN)) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            requestRepository.save(request);
            return ResponseEntity.ok(request);
        }

        return ResponseEntity.status(403).body("Обработка недоступна");
    }
}