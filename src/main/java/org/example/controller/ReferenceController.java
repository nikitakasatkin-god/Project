package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ProductRepository productRepository;

    public ReferenceController(DivisionRepository divisionRepository,
                               CarrierRepository carrierRepository,
                               PlantRepository plantRepository,
                               WarehouseRepository warehouseRepository,
                               TariffBrandedRepository tariffBrandedRepository,
                               TariffNonBrandedRepository tariffNonBrandedRepository,
                               VehicleRepository vehicleRepository,
                               ProductRepository productRepository) {
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.tariffBrandedRepository = tariffBrandedRepository;
        this.tariffNonBrandedRepository = tariffNonBrandedRepository;
        this.vehicleRepository = vehicleRepository;
        this.productRepository = productRepository;
    }

    // ========== Divisions ==========
    @GetMapping("/divisions")
    public List<Division> getDivisions() {
        return divisionRepository.findAll();
    }

    @PostMapping("/divisions")
    public ResponseEntity<?> createDivision(@RequestBody Division division) {
        return ResponseEntity.ok(divisionRepository.save(division));
    }

    @PutMapping("/divisions/{id}")
    public ResponseEntity<?> updateDivision(@PathVariable Long id, @RequestBody Division division) {
        division.setId(id);
        return ResponseEntity.ok(divisionRepository.save(division));
    }

    @DeleteMapping("/divisions/{id}")
    public ResponseEntity<?> deleteDivision(@PathVariable Long id) {
        divisionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Plants (Заводы) ==========
    @GetMapping("/plants")
    public List<Plant> getPlants() {
        return plantRepository.findAll();
    }

    @GetMapping("/plants/{id}")
    public ResponseEntity<Plant> getPlantById(@PathVariable Long id) {
        return plantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/plants")
    public ResponseEntity<?> createPlant(@RequestBody Plant plant) {
        if (plant.getName() == null || plant.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Название завода обязательно"));
        }
        return ResponseEntity.ok(plantRepository.save(plant));
    }

    @PutMapping("/plants/{id}")
    public ResponseEntity<?> updatePlant(@PathVariable Long id, @RequestBody Plant plant) {
        if (!plantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        plant.setId(id);
        return ResponseEntity.ok(plantRepository.save(plant));
    }

    @DeleteMapping("/plants/{id}")
    public ResponseEntity<?> deletePlant(@PathVariable Long id) {
        if (!plantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        plantRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Warehouses (Склады) ==========
    @GetMapping("/warehouses")
    public List<Warehouse> getWarehouses() {
        return warehouseRepository.findAll();
    }

    @GetMapping("/warehouses/{id}")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable Long id) {
        return warehouseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/warehouses")
    public ResponseEntity<?> createWarehouse(@RequestBody Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Название склада обязательно"));
        }
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<?> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        if (!warehouseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        warehouse.setId(id);
        return ResponseEntity.ok(warehouseRepository.save(warehouse));
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Long id) {
        if (!warehouseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        warehouseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Carriers ==========
    @GetMapping("/carriers")
    public List<Carrier> getCarriers() {
        return carrierRepository.findAll();
    }

    @GetMapping("/carriers/{id}")
    public ResponseEntity<?> getCarrierById(@PathVariable Long id) {
        Carrier carrier = carrierRepository.findById(id).orElse(null);
        if (carrier == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", carrier.getId());
        response.put("name", carrier.getName());
        response.put("contactPerson", carrier.getContactPerson());
        response.put("phone", carrier.getPhone());
        response.put("email", carrier.getEmail());

        List<Vehicle> vehicles = vehicleRepository.findByCarrier(carrier);
        List<Map<String, Object>> vehicleList = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (v.getPlateNumber() == null && v.getTrailerPlate() == null && v.getDriverName() == null) {
                continue;
            }
            Map<String, Object> vehicleMap = new HashMap<>();
            vehicleMap.put("id", v.getId());
            vehicleMap.put("plateNumber", v.getPlateNumber() != null ? v.getPlateNumber() : "");
            vehicleMap.put("brand", v.getBrand() != null ? v.getBrand() : "");
            vehicleMap.put("model", v.getModel() != null ? v.getModel() : "");
            vehicleMap.put("trailerPlate", v.getTrailerPlate() != null ? v.getTrailerPlate() : "");
            vehicleMap.put("driverName", v.getDriverName() != null ? v.getDriverName() : "");
            vehicleMap.put("driverPhone", v.getDriverPhone() != null ? v.getDriverPhone() : "");
            vehicleMap.put("driverEmail", v.getDriverEmail() != null ? v.getDriverEmail() : "");
            vehicleMap.put("isTrailer", v.getIsTrailer() != null ? v.getIsTrailer() : false);
            vehicleMap.put("isDriver", v.getIsDriver() != null ? v.getIsDriver() : false);
            vehicleList.add(vehicleMap);
        }
        response.put("vehicles", vehicleList);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/carriers/{id}/vehicles")
    public List<Vehicle> getCarrierVehicles(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .filter(v -> v.getPlateNumber() != null && !v.getPlateNumber().trim().isEmpty())
                        .toList())
                .orElse(List.of());
    }

    @GetMapping("/carriers/{id}/plate-numbers")
    public List<String> getCarrierPlateNumbers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getPlateNumber)
                        .filter(p -> p != null && !p.trim().isEmpty())
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
                        .filter(trailer -> trailer != null && !trailer.trim().isEmpty())
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
                        .filter(driver -> driver != null && !driver.trim().isEmpty())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    @PostMapping("/carriers")
    public ResponseEntity<?> createCarrier(@RequestBody Carrier carrier) {
        return ResponseEntity.ok(carrierRepository.save(carrier));
    }

    @PutMapping("/carriers/{id}")
    public ResponseEntity<?> updateCarrier(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Carrier carrier = carrierRepository.findById(id).orElse(null);
        if (carrier == null) {
            return ResponseEntity.notFound().build();
        }

        if (data.containsKey("name")) {
            carrier.setName(data.get("name").toString());
        }
        if (data.containsKey("contactPerson")) {
            carrier.setContactPerson(data.get("contactPerson").toString());
        }
        if (data.containsKey("phone")) {
            carrier.setPhone(data.get("phone").toString());
        }
        if (data.containsKey("email")) {
            carrier.setEmail(data.get("email").toString());
        }

        return ResponseEntity.ok(carrierRepository.save(carrier));
    }

    @DeleteMapping("/carriers/{id}")
    public ResponseEntity<?> deleteCarrier(@PathVariable Long id) {
        carrierRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Vehicles ==========
    @PostMapping("/carriers/{carrierId}/vehicles")
    public ResponseEntity<?> addVehicle(@PathVariable Long carrierId, @RequestBody Map<String, Object> data) {
        Carrier carrier = carrierRepository.findById(carrierId).orElse(null);
        if (carrier == null) {
            return ResponseEntity.notFound().build();
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setCarrier(carrier);

        if (data.containsKey("plateNumber") && data.get("plateNumber") != null && !data.get("plateNumber").toString().isEmpty()) {
            String plateNumber = data.get("plateNumber").toString();
            if (plateNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Госномер обязателен"));
            }
            boolean exists = vehicleRepository.findAll().stream()
                    .anyMatch(v -> v.getPlateNumber() != null && v.getPlateNumber().equals(plateNumber));
            if (exists) {
                return ResponseEntity.badRequest().body(Map.of("error", "Автомобиль с таким госномером уже существует"));
            }
            vehicle.setPlateNumber(plateNumber);
            vehicle.setBrand(data.get("brand") != null ? data.get("brand").toString() : "");
            vehicle.setModel(data.get("model") != null ? data.get("model").toString() : "");
        }

        if (data.containsKey("trailerPlate") && data.get("trailerPlate") != null && !data.get("trailerPlate").toString().isEmpty()) {
            String trailerPlate = data.get("trailerPlate").toString();
            if (trailerPlate.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Госномер прицепа обязателен"));
            }
            vehicle.setTrailerPlate(trailerPlate);
            vehicle.setIsTrailer(true);
        }

        if (data.containsKey("driverName") && data.get("driverName") != null && !data.get("driverName").toString().isEmpty()) {
            vehicle.setDriverName(data.get("driverName").toString());
            if (data.containsKey("driverPhone")) {
                vehicle.setDriverPhone(data.get("driverPhone").toString());
            }
            if (data.containsKey("driverEmail")) {
                vehicle.setDriverEmail(data.get("driverEmail").toString());
            }
            vehicle.setIsDriver(true);
        }

        if ((vehicle.getPlateNumber() == null || vehicle.getPlateNumber().isEmpty()) &&
                (vehicle.getTrailerPlate() == null || vehicle.getTrailerPlate().isEmpty()) &&
                (vehicle.getDriverName() == null || vehicle.getDriverName().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Не указаны данные для сохранения"));
        }

        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @PutMapping("/carriers/{carrierId}/vehicles/{vehicleId}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long carrierId, @PathVariable Long vehicleId, @RequestBody Map<String, Object> data) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null || !vehicle.getCarrier().getId().equals(carrierId)) {
            return ResponseEntity.notFound().build();
        }

        if (data.containsKey("plateNumber")) {
            vehicle.setPlateNumber(data.get("plateNumber").toString());
        }
        if (data.containsKey("brand")) {
            vehicle.setBrand(data.get("brand").toString());
        }
        if (data.containsKey("model")) {
            vehicle.setModel(data.get("model").toString());
        }
        if (data.containsKey("trailerPlate")) {
            vehicle.setTrailerPlate(data.get("trailerPlate").toString());
        }
        if (data.containsKey("driverName")) {
            vehicle.setDriverName(data.get("driverName").toString());
        }
        if (data.containsKey("driverPhone")) {
            vehicle.setDriverPhone(data.get("driverPhone").toString());
        }
        if (data.containsKey("driverEmail")) {
            vehicle.setDriverEmail(data.get("driverEmail").toString());
        }

        return ResponseEntity.ok(vehicleRepository.save(vehicle));
    }

    @DeleteMapping("/carriers/{carrierId}/vehicles/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long carrierId, @PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null || !vehicle.getCarrier().getId().equals(carrierId)) {
            return ResponseEntity.notFound().build();
        }

        vehicleRepository.delete(vehicle);
        return ResponseEntity.ok().build();
    }

    // ========== Products ==========
    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Название продукта обязательно"));
        }

        Optional<Product> existing = productRepository.findByName(product.getName());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Продукт с таким названием уже существует"));
        }

        product.setActive(true);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        if (data.containsKey("name")) {
            String newName = data.get("name").toString();
            if (newName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Название продукта не может быть пустым"));
            }
            Optional<Product> existing = productRepository.findByName(newName);
            if (existing.isPresent() && existing.get().getId() != id) {
                return ResponseEntity.badRequest().body(Map.of("error", "Продукт с таким названием уже существует"));
            }
            product.setName(newName);
        }

        if (data.containsKey("description")) {
            product.setDescription(data.get("description").toString());
        }

        if (data.containsKey("active")) {
            product.setActive(Boolean.parseBoolean(data.get("active").toString()));
        }

        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ========== Tariffs ==========
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
}