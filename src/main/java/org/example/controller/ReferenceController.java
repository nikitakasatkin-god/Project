package org.example.controller;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    public ReferenceController(DivisionRepository divisionRepository,
                               CarrierRepository carrierRepository,
                               PlantRepository plantRepository,
                               WarehouseRepository warehouseRepository,
                               TariffBrandedRepository tariffBrandedRepository,
                               TariffNonBrandedRepository tariffNonBrandedRepository,
                               VehicleRepository vehicleRepository) {
        this.divisionRepository = divisionRepository;
        this.carrierRepository = carrierRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.tariffBrandedRepository = tariffBrandedRepository;
        this.tariffNonBrandedRepository = tariffNonBrandedRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // Divisions
    @GetMapping("/divisions")
    public List<Division> getDivisions() { return divisionRepository.findAll(); }

    @PostMapping("/divisions")
    public Division createDivision(@RequestBody Division division) { return divisionRepository.save(division); }

    @PutMapping("/divisions/{id}")
    public Division updateDivision(@PathVariable Long id, @RequestBody Division division) {
        division.setId(id);
        return divisionRepository.save(division);
    }

    @DeleteMapping("/divisions/{id}")
    public void deleteDivision(@PathVariable Long id) { divisionRepository.deleteById(id); }

    // Carriers
    @GetMapping("/carriers")
    public List<Carrier> getCarriers() { return carrierRepository.findAll(); }

    @GetMapping("/carriers/{id}/vehicles")
    public List<Vehicle> getCarrierVehicles(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(vehicleRepository::findByCarrier)
                .orElse(List.of());
    }

    // ДОБАВЬТЕ - получение всех автомобилей перевозчика с уникальными номерами
    @GetMapping("/carriers/{id}/plate-numbers")
    public List<String> getCarrierPlateNumbers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getPlateNumber)
                        .distinct()
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    // ДОБАВЬТЕ - получение всех прицепов перевозчика (уникальных)
    @GetMapping("/carriers/{id}/trailers")
    public List<String> getCarrierTrailers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getTrailerPlate)
                        .filter(trailer -> trailer != null && !trailer.isEmpty())
                        .distinct()
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    // ДОБАВЬТЕ - получение всех водителей перевозчика (уникальных)
    @GetMapping("/carriers/{id}/drivers")
    public List<String> getCarrierDrivers(@PathVariable Long id) {
        return carrierRepository.findById(id)
                .map(carrier -> vehicleRepository.findByCarrier(carrier)
                        .stream()
                        .map(Vehicle::getDriverName)
                        .filter(driver -> driver != null && !driver.isEmpty())
                        .distinct()
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @PostMapping("/carriers")
    public Carrier createCarrier(@RequestBody Carrier carrier) { return carrierRepository.save(carrier); }

    // Plants
    @GetMapping("/plants")
    public List<Plant> getPlants() { return plantRepository.findAll(); }

    @PostMapping("/plants")
    public Plant createPlant(@RequestBody Plant plant) { return plantRepository.save(plant); }

    // Warehouses
    @GetMapping("/warehouses")
    public List<Warehouse> getWarehouses() { return warehouseRepository.findAll(); }

    @PostMapping("/warehouses")
    public Warehouse createWarehouse(@RequestBody Warehouse warehouse) { return warehouseRepository.save(warehouse); }

    // Tariffs
    @GetMapping("/tariffs/branded")
    public List<TariffBranded> getTariffsBranded() { return tariffBrandedRepository.findAll(); }

    @PostMapping("/tariffs/branded")
    public TariffBranded createTariffBranded(@RequestBody TariffBranded tariff) { return tariffBrandedRepository.save(tariff); }

    @GetMapping("/tariffs/non-branded")
    public List<TariffNonBranded> getTariffsNonBranded() { return tariffNonBrandedRepository.findAll(); }

    @PostMapping("/tariffs/non-branded")
    public TariffNonBranded createTariffNonBranded(@RequestBody TariffNonBranded tariff) { return tariffNonBrandedRepository.save(tariff); }
}