package org.example.controller;

import org.example.config.IntegrationSettings;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.OneCIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/references")
public class ReferenceController {

    private final DivisionRepository divisionRepository;
    private final CarrierRepository carrierRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final TariffBrandedRepository tariffBrandedRepository;
    private final TariffNonBrandedRepository tariffNonBrandedRepository;
    private final VehicleRepository vehicleRepository;
    private final IntegrationSettings integrationSettings;
    private final OneCIntegrationService oneCIntegrationService;

    public ReferenceController(DivisionRepository divisionRepository,
                               CarrierRepository carrierRepository,
                               PlantRepository plantRepository,
                               WarehouseRepository warehouseRepository,
                               TariffBrandedRepository tariffBrandedRepository,
                               TariffNonBrandedRepository tariffNonBrandedRepository,
                               VehicleRepository vehicleRepository,
                               IntegrationSettings integrationSettings,
                               OneCIntegrationService oneCIntegrationService) {
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.tariffBrandedRepository = tariffBrandedRepository;
        this.tariffNonBrandedRepository = tariffNonBrandedRepository;
        this.vehicleRepository = vehicleRepository;
        this.integrationSettings = integrationSettings;
        this.oneCIntegrationService = oneCIntegrationService;
    }

    private boolean isIntegrationMode() {
        return integrationSettings.isOnecIntegrationEnabled();
    }

    private ResponseEntity<?> checkWritePermissionForDirectory() {
        if (isIntegrationMode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Режим интеграции с 1С активен. Редактирование справочников (подразделения, заводы, склады, перевозчики) запрещено. Данные синхронизируются из 1С."));
        }
        return null;
    }

    // ========== Divisions ==========
    @GetMapping("/divisions")
    public List<Division> getDivisions() {
        return divisionRepository.findAll();
    }

    @PostMapping("/divisions")
    public ResponseEntity<?> createDivision(@RequestBody Division division) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        return ResponseEntity.ok(divisionRepository.save(division));
    }

    @PutMapping("/divisions/{id}")
    public ResponseEntity<?> updateDivision(@PathVariable Long id, @RequestBody Division division) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        division.setId(id);
        return ResponseEntity.ok(divisionRepository.save(division));
    }

    @DeleteMapping("/divisions/{id}")
    public ResponseEntity<?> deleteDivision(@PathVariable Long id) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        divisionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Carriers ==========
    @GetMapping("/carriers")
    public List<Carrier> getCarriers() {
        return carrierRepository.findAll();
    }

    @GetMapping("/carriers/{id}/vehicles")
    public List<Vehicle> getCarrierVehicles(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(vehicleRepository::findByCarrier)
                .orElse(List.of());
    }

    @GetMapping("/carriers/{id}/plate-numbers")
    public List<String> getCarrierPlateNumbers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getPlateNumber)
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    @GetMapping("/carriers/{id}/trailers")
    public List<String> getCarrierTrailers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getTrailerPlate)
                        .filter(trailer -> trailer != null && !trailer.isEmpty())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    @GetMapping("/carriers/{id}/drivers")
    public List<String> getCarrierDrivers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getDriverName)
                        .filter(driver -> driver != null && !driver.isEmpty())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    @PostMapping("/carriers")
    public ResponseEntity<?> createCarrier(@RequestBody Carrier carrier) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        return ResponseEntity.ok(carrierRepository.save(carrier));
    }

    @PutMapping("/carriers/{id}")
    public ResponseEntity<?> updateCarrier(@PathVariable Long id, @RequestBody Carrier carrier) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        carrier.setId(id);
        return ResponseEntity.ok(carrierRepository.save(carrier));
    }

    @DeleteMapping("/carriers/{id}")
    public ResponseEntity<?> deleteCarrier(@PathVariable Long id) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        carrierRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Plants ==========
    @GetMapping("/plants")
    public List<Plant> getPlants() {
        return plantRepository.findAll();
    }

    @PostMapping("/plants")
    public ResponseEntity<?> createPlant(@RequestBody Plant plant) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        return ResponseEntity.ok(plantRepository.save(plant));
    }

    @PutMapping("/plants/{id}")
    public ResponseEntity<?> updatePlant(@PathVariable Long id, @RequestBody Plant plant) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        plant.setId(id);
        return ResponseEntity.ok(plantRepository.save(plant));
    }

    @DeleteMapping("/plants/{id}")
    public ResponseEntity<?> deletePlant(@PathVariable Long id) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        plantRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Warehouses ==========
    @GetMapping("/warehouses")
    public List<Warehouse> getWarehouses() {
        return warehouseRepository.findAll();
    }

    @PostMapping("/warehouses")
    public ResponseEntity<?> createWarehouse(@RequestBody Warehouse warehouse) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        warehouse.setId(id);
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id) {
        ResponseEntity<?> permissionCheck = checkWritePermissionForDirectory();
        if (permissionCheck != null) return permissionCheck;
        warehouseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Tariffs (всегда доступны для редактирования) ==========
    @GetMapping("/tariffs/branded")
    public List<TariffBranded> getTariffsBranded() {
        return tariffBrandedRepository.findAll();
    }

    @PostMapping("/tariffs/branded")
    public TariffBranded createTariffBranded(@RequestBody TariffBranded tariff) {
        return tariffBrandedRepository.save(tariff);
    }

    @GetMapping("/tariffs/non-branded")
    public List<TariffNonBranded> getTariffsNonBranded() {
        return tariffNonBrandedRepository.findAll();
    }

    @PostMapping("/tariffs/non-branded")
    public TariffNonBranded createTariffNonBranded(@RequestBody TariffNonBranded tariff) {
        return tariffNonBrandedRepository.save(tariff);
    }

    // ========== Синхронизация с 1С ==========
    @PostMapping("/sync-from-1c")
    public ResponseEntity<?> syncFromOneC() {
        if (!isIntegrationMode()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Интеграция с 1С отключена"));
        }
        oneCIntegrationService.syncAllDirectories();
        return ResponseEntity.ok(Map.of("success", true, "message", "Синхронизация с 1С запущена"));
    }
}