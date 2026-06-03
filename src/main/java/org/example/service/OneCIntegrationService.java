package org.example.service;

import org.example.config.IntegrationSettings;
import org.example.model.*;
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OneCIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(OneCIntegrationService.class);

    private final IntegrationSettings settings;
    private final RestTemplate restTemplate = new RestTemplate();

    private final DivisionRepository divisionRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final CarrierRepository carrierRepository;
    private final VehicleRepository vehicleRepository;

    public OneCIntegrationService(IntegrationSettings settings,
                                  DivisionRepository divisionRepository,
                                  PlantRepository plantRepository,
                                  WarehouseRepository warehouseRepository,
                                  CarrierRepository carrierRepository,
                                  VehicleRepository vehicleRepository) {
        this.settings = settings;
        this.divisionRepository = divisionRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.carrierRepository = carrierRepository;
        this.vehicleRepository = vehicleRepository;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = settings.getOnecUsername() + ":" + settings.getOnecPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        return headers;
    }

    public void syncAllDirectories() {
        if (!settings.isOnecIntegrationEnabled()) {
            log.info("Интеграция с 1С отключена");
            return;
        }

        log.info("=== СИНХРОНИЗАЦИЯ СПРАВОЧНИКОВ ИЗ 1С ===");

        syncDivisions();
        syncPlants();
        syncWarehouses();
        syncCarriers();

        log.info("=== СИНХРОНИЗАЦИЯ ЗАВЕРШЕНА ===");
    }

    @SuppressWarnings("unchecked")
    public void syncDivisions() {
        try {
            String url = settings.getOnecApiUrl() + "/divisions";
            HttpEntity<?> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> data : response.getBody()) {
                    String name = data.get("name").toString();
                    Division existing = divisionRepository.findByName(name).orElse(null);

                    if (existing == null) {
                        Division division = new Division();
                        division.setName(name);
                        division.setDescription((String) data.get("description"));
                        divisionRepository.save(division);
                        log.info("Создано подразделение: {}", division.getName());
                    } else {
                        // Обновляем описание, если изменилось
                        String description = data.get("description") != null ? data.get("description").toString() : null;
                        if (description != null && !description.equals(existing.getDescription())) {
                            existing.setDescription(description);
                            divisionRepository.save(existing);
                            log.info("Обновлено подразделение: {}", existing.getName());
                        }
                    }
                }
                log.info("Синхронизировано подразделений: {}", response.getBody().size());
            }
        } catch (Exception e) {
            log.error("Ошибка синхронизации подразделений: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void syncPlants() {
        try {
            String url = settings.getOnecApiUrl() + "/plants";
            HttpEntity<?> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> data : response.getBody()) {
                    Plant plant = new Plant();
                    plant.setName(data.get("name").toString());
                    plant.setAddress((String) data.get("address"));
                    plant.setContactPerson((String) data.get("contactPerson"));
                    plant.setPhone((String) data.get("phone"));
                    plantRepository.save(plant);
                    log.info("Создан завод: {}", plant.getName());
                }
                log.info("Синхронизировано заводов: {}", response.getBody().size());
            }
        } catch (Exception e) {
            log.error("Ошибка синхронизации заводов: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void syncWarehouses() {
        try {
            String url = settings.getOnecApiUrl() + "/warehouses";
            HttpEntity<?> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> data : response.getBody()) {
                    Warehouse warehouse = new Warehouse();
                    warehouse.setName(data.get("name").toString());
                    warehouse.setAddress((String) data.get("address"));
                    warehouse.setContactPerson((String) data.get("contactPerson"));
                    warehouse.setPhone((String) data.get("phone"));
                    warehouseRepository.save(warehouse);
                    log.info("Создан склад: {}", warehouse.getName());
                }
                log.info("Синхронизировано складов: {}", response.getBody().size());
            }
        } catch (Exception e) {
            log.error("Ошибка синхронизации складов: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void syncCarriers() {
        try {
            String url = settings.getOnecApiUrl() + "/carriers";
            HttpEntity<?> request = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (Map<String, Object> data : response.getBody()) {
                    Carrier carrier = new Carrier();
                    carrier.setName(data.get("name").toString());
                    carrier.setContactPerson((String) data.get("contactPerson"));
                    carrier.setPhone((String) data.get("phone"));
                    carrier.setEmail((String) data.get("email"));
                    Carrier savedCarrier = carrierRepository.save(carrier);
                    log.info("Создан перевозчик: {}", savedCarrier.getName());

                    if (data.containsKey("vehicles")) {
                        List<Map<String, Object>> vehicles = (List<Map<String, Object>>) data.get("vehicles");
                        for (Map<String, Object> vehicleData : vehicles) {
                            Vehicle vehicle = new Vehicle();
                            vehicle.setPlateNumber(vehicleData.get("plateNumber").toString());
                            vehicle.setBrand((String) vehicleData.get("brand"));
                            vehicle.setModel((String) vehicleData.get("model"));
                            vehicle.setDriverName((String) vehicleData.get("driverName"));
                            vehicle.setTrailerPlate((String) vehicleData.get("trailerPlate"));
                            vehicle.setCarrier(savedCarrier);
                            vehicleRepository.save(vehicle);
                            log.info("  - Добавлен транспорт: {}", vehicle.getPlateNumber());
                        }
                    }
                }
                log.info("Синхронизировано перевозчиков: {}", response.getBody().size());
            }
        } catch (Exception e) {
            log.error("Ошибка синхронизации перевозчиков: {}", e.getMessage());
        }
    }
}